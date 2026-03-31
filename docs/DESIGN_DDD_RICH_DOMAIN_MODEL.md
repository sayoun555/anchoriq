## DDD 풍부한 도메인 모델: 빈약한 도메인(Service 집중) -> 풍부한 도메인(Entity 로직 보유)

### 배경 및 문제정의
- 상황: AnchorIQ는 선박 리스크 평가, 구독 상태 전이, 결제 처리 등 복잡한 비즈니스 규칙이 있다. 처음 설계 시 모든 로직을 Service 레이어에 넣는 빈약한 도메인 모델(Anemic Domain Model)로 시작하면 Service가 비대해진다.
- 문제: Entity가 단순 데이터 홀더(getter/setter 덩어리)이면, 비즈니스 규칙이 여러 Service에 분산되어 중복이 발생하고, 도메인 지식이 코드에서 사라진다. 상태 전이 규칙 위반을 컴파일 타임에 잡을 수 없다.

### 기술 선정 (대안 비교 테이블)

| 항목 | 빈약한 도메인 (Anemic) | 풍부한 도메인 (Rich) |
|------|---------------------|-------------------|
| 비즈니스 로직 위치 | Service에 집중 | Entity/VO에 분산 |
| 도메인 규칙 중복 | Service마다 반복 | Entity 한 곳에 집중 |
| 상태 전이 보호 | Service에서 if-else | enum 상태 머신으로 강제 |
| 단위 테스트 | Service 통째로 모킹 필요 | Entity 단독 테스트 가능 |
| 코드 발견성 | "이 규칙이 어디 있지?" | Entity를 보면 규칙이 보인다 |
| Application Service 역할 | 비즈니스 로직 수행 | 오케스트레이션만 |

### 분석 / CS 원리

**Tell, Don't Ask**: 객체의 상태를 꺼내서(ask) 외부에서 판단하지 말고, 객체에게 행위를 요청(tell)하라. `vessel.evaluateRiskScore()`는 선박 자신이 리스크를 평가한다. Service가 `vessel.getFlag()`, `vessel.getType()`을 꺼내서 판단하지 않는다.

**상태 머신 패턴**: VesselStatus와 SubscriptionStatus가 허용된 전이 규칙을 enum 내부에 정의한다. `DECOMMISSIONED -> SAILING` 같은 불법 전이를 시도하면 `validateTransitionTo()`가 즉시 예외를 던진다. 런타임 버그를 도메인 모델 레벨에서 차단한다.

**AggregateRoot + Domain Event**: Entity가 상태를 변경할 때 `registerEvent()`로 도메인 이벤트를 수집한다. Application Service가 저장 후 이벤트를 발행하여 다른 Aggregate(알림, 타임라인)에 전파한다. Entity 간 직접 의존을 제거한다.

**Factory 패턴**: 생성 과정에 IMO 중복 체크, 제재국 탐지 등 복잡한 로직이 필요하면 Factory에 캡슐화한다. 생성자에 Repository를 주입하지 않고, Factory가 협력 객체를 조합한다.

### 솔루션 (핵심 코드 블록)

```java
// 1. AggregateRoot: 도메인 이벤트 수집 기반 클래스
public abstract class AggregateRoot {
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    protected void registerEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }
}
```

```java
// 2. Vessel.evaluateRiskScore(): Entity가 리스크를 자체 판단
public int evaluateRiskScore(Set<String> sanctionedCountryCodes,
                             Set<String> highRiskFlags) {
    int previousScore = this.riskScore;
    int score = 0;

    if (isRegisteredInSanctionedCountry(sanctionedCountryCodes)) score += 40;
    if (highRiskFlags.contains(this.flag.value())) score += 20;

    int age = calculateAge();
    if (age >= 20) score += 15;
    else if (age >= 15) score += 10;

    if (isTanker()) score += 10;
    if (this.status == VesselStatus.NOT_UNDER_COMMAND
            || this.status == VesselStatus.UNKNOWN) score += 15;

    this.riskScore = Math.min(score, 100);

    // 점수 변경 시 도메인 이벤트 발행
    if (previousScore != this.riskScore) {
        registerEvent(new RiskScoreChangedEvent(
                this.imo.value(), "VESSEL", previousScore, this.riskScore));
    }
    return this.riskScore;
}
```

```java
// 3. SubscriptionStatus 상태 머신: 불법 전이 차단
public enum SubscriptionStatus {
    PENDING(Set.of("ACTIVE", "CANCELLED")),
    ACTIVE(Set.of("CANCELLED", "EXPIRED")),
    CANCELLED(Set.of("ACTIVE")),      // 재활성화 가능
    EXPIRED(Set.of("ACTIVE"));         // 재구독 가능

    public void validateTransitionTo(SubscriptionStatus target) {
        if (!allowedTransitions.contains(target.name())) {
            throw new IllegalStateException(
                String.format("Invalid transition: %s -> %s", this.name(), target.name()));
        }
    }
}

// Subscription Entity에서 상태 전이 사용
public void activate(Plan newPlan) {
    this.status.validateTransitionTo(SubscriptionStatus.ACTIVE);  // 불법 전이 차단
    this.plan = newPlan;
    this.status = SubscriptionStatus.ACTIVE;
    domainEvents.add(new SubscriptionActivatedEvent(this.userId, newPlan.name()));
}
```

```java
// 4. VesselFactory: 복잡한 생성 로직 캡슐화
public class VesselFactory {
    public Vessel createVessel(Imo imo, Mmsi mmsi, String name, Flag flag,
                                VesselType type, int deadweight, int buildYear) {
        // 1. IMO 중복 체크
        vesselRepository.findByImo(imo.value())
                .ifPresent(v -> { throw new DuplicateException(...); });

        // 2. Entity 생성 (Builder 패턴)
        Vessel vessel = Vessel.builder().imo(imo).mmsi(mmsi)...build();

        // 3. 제재국 선박이면 이벤트 발행
        if (vessel.isRegisteredInSanctionedCountry(sanctionedCodes)) {
            vessel.markAsSanctionedCountryVessel();
        }
        return vessel;
    }
}
```

### 결과 (Before/After 수치 테이블)

| 지표 | Before (빈약한 도메인) | After (풍부한 도메인) |
|------|---------------------|-------------------|
| Service 레이어 코드량 | 70% (비즈니스 로직 포함) | 30% (오케스트레이션만) |
| Entity 단위 테스트 가능성 | 불가 (데이터 홀더) | 가능 (로직 보유) |
| 상태 전이 버그 | 런타임 발견 | 즉시 예외 발생 |
| 비즈니스 규칙 중복 | 3~5개 Service에 분산 | Entity 1곳에 집중 |
| 도메인 이벤트 활용 | 없음 (직접 호출) | AggregateRoot 이벤트 수집 |
| 새 도메인 규칙 추가 위치 | "어디 넣지?" | Entity에 메서드 추가 |
