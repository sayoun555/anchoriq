## AggregateRoot + Domain Event + 상태 머신 적용: 이벤트 없는 직접 호출 → 이벤트 기반 사이드 이펙트 + 상태 전이 검증

### 배경 및 문제정의
- 상황: AnchorIQ의 핵심 엔티티(Vessel, Subscription, Sanction 등)가 상태를 변경할 때 관련 사이드 이펙트(알림 발송, 감사 로그 기록, 캐시 무효화 등)를 직접 호출하고 있었다. 상태 전이에 대한 규칙도 없었다.
- 문제:
  - **도메인 이벤트 누락**: 선박 상태가 변경되면 알림을 보내야 하고, 리스크 점수가 변경되면 대시보드를 갱신해야 한다. 이런 사이드 이펙트를 Service 레이어에서 직접 호출하면 결합도가 높아지고, 새로운 사이드 이펙트 추가 시 기존 코드를 수정해야 한다.
  - **잘못된 상태 전이 허용**: 선박이 `DECOMMISSIONED`(폐선) 상태에서 `SAILING`으로 변경되거나, 구독이 `EXPIRED`에서 바로 `CANCELLED`로 전이되는 것이 방지되지 않았다.
  - **감사 추적 불가**: 어떤 상태에서 어떤 상태로 변경되었는지 이력이 남지 않아, 문제 발생 시 원인 추적이 불가능했다.

### 기술 선정 (대안 비교 테이블)

| 항목 | 이벤트 없이 직접 호출 | Spring ApplicationEvent | AggregateRoot 이벤트 수집 |
|------|---------------------|------------------------|--------------------------|
| 결합도 | 높음 (사이드 이펙트 직접 의존) | 낮음 (이벤트 리스너로 분리) | 낮음 (도메인 내부에서 이벤트 수집) |
| 트랜잭션 범위 | 호출부에서 관리 | @TransactionalEventListener | 이벤트 수집 후 일괄 발행 |
| 도메인 순수성 | 해당 없음 | Spring 의존성 필요 | 순수 POJO (Spring 의존 없음) |
| 이벤트 유실 위험 | 높음 (수동 호출 누락) | 낮음 | 낮음 (AggregateRoot가 수집) |
| 테스트 | 사이드 이펙트 Mock 필요 | Spring Context 필요 | 단위 테스트로 이벤트 검증 가능 |

**선택: AggregateRoot 이벤트 수집 패턴**. domain 모듈에 Spring 의존성을 넣지 않는 프로젝트 규칙(AGENTS.md 8번)을 지키면서, 도메인 이벤트를 순수 POJO로 구현할 수 있다. Application Service에서 이벤트를 꺼내 Spring Event로 발행하는 방식으로 도메인 순수성과 이벤트 기반 아키텍처를 동시에 달성한다.

### 분석 / CS 원리

**Aggregate Root 패턴 (Eric Evans, DDD)**

Aggregate Root는 트랜잭션 일관성의 경계이다. 외부에서 Aggregate 내부 객체를 직접 수정하면 일관성이 깨질 수 있으므로, 모든 변경은 Root를 통해서만 이루어진다. Root가 변경 시 이벤트를 "수집"하고, 트랜잭션 커밋 시점에 일괄 발행하면:

1. **도메인 로직과 사이드 이펙트 분리**: Entity는 이벤트를 등록만 하고, 실제 처리는 리스너가 담당한다.
2. **새 사이드 이펙트 추가 시 OCP 준수**: 기존 코드 수정 없이 새 EventListener만 추가하면 된다.

**상태 머신 패턴 (Finite State Machine)**

상태 전이를 enum 내부에서 정의하면:
- 허용된 전이만 가능 (화이트리스트 방식)
- 잘못된 전이 시 즉시 예외 발생 (fail-fast)
- 상태 전이 규칙이 코드에 명시적으로 문서화됨

`DECOMMISSIONED`(폐선)은 `Set.of()`로 빈 전이 집합을 가지므로 종료 상태(Terminal State)가 된다. 어떤 상태에서도 폐선으로 갈 수 있지만, 폐선에서는 어디로도 갈 수 없다.

### 솔루션 (핵심 코드 블록)

**1. AggregateRoot 기반 클래스** — 이벤트 수집 책임:

```java
// domain/common/model/AggregateRoot.java
public abstract class AggregateRoot {

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    // 하위 클래스에서 이벤트를 등록한다
    protected void registerEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    // Application Service가 이벤트를 꺼내 발행한다
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    // 발행 후 이벤트를 비운다
    public void clearDomainEvents() {
        this.domainEvents.clear();
    }
}
```

**2. DomainEvent 기반 클래스** — 이벤트 ID + 발생 시각 보장:

```java
// domain/common/event/DomainEvent.java
public abstract class DomainEvent {

    private final String eventId;      // UUID로 고유성 보장
    private final Instant occurredAt;  // 이벤트 발생 시각

    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = Instant.now();
    }
}
```

**3. VesselStatus 상태 머신** — 유효한 전이만 허용:

```java
// domain/maritime/vessel/model/VesselStatus.java
public enum VesselStatus {

    SAILING(Set.of("MOORED", "ANCHORED", "DECOMMISSIONED")),
    ANCHORED(Set.of("SAILING", "DECOMMISSIONED")),
    MOORED(Set.of("SAILING", "DECOMMISSIONED")),
    NOT_UNDER_COMMAND(Set.of("SAILING", "MOORED", "ANCHORED", "DECOMMISSIONED")),
    UNKNOWN(Set.of("SAILING", "MOORED", "ANCHORED", "DECOMMISSIONED")),
    DECOMMISSIONED(Set.of()); // 종료 상태 — 전이 불가

    private final Set<String> allowedTransitions;

    // 불가능한 전이 시 즉시 예외
    public void validateTransitionTo(VesselStatus target) {
        if (!allowedTransitions.contains(target.name())) {
            throw new IllegalStateException(
                String.format("Invalid vessel status transition: %s -> %s",
                    this.name(), target.name()));
        }
    }
}
```

**4. Vessel.changeStatus()** — 상태 머신 + 이벤트 발행 통합:

```java
// domain/maritime/vessel/model/Vessel.java
public class Vessel extends AggregateRoot {

    public void changeStatus(VesselStatus newStatus) {
        Objects.requireNonNull(newStatus, "New status must not be null");

        // 1. 상태 머신으로 유효성 검증 (잘못된 전이 시 예외)
        this.status.validateTransitionTo(newStatus);

        // 2. 상태 변경
        String previousStatusName = this.status.name();
        this.status = newStatus;
        this.lastUpdated = Instant.now();

        // 3. 도메인 이벤트 등록 (사이드 이펙트는 리스너가 처리)
        registerEvent(new VesselStatusChangedEvent(
                this.imo.value(), previousStatusName, newStatus.name()));
    }

    public int evaluateRiskScore(Set<String> sanctionedCountryCodes,
                                  Set<String> highRiskFlags) {
        int previousScore = this.riskScore;
        // ... 리스크 점수 계산 로직 ...
        this.riskScore = Math.min(score, 100);

        // 점수가 변경된 경우에만 이벤트 발행
        if (previousScore != this.riskScore) {
            registerEvent(new RiskScoreChangedEvent(
                    this.imo.value(), "VESSEL", previousScore, this.riskScore));
        }
        return this.riskScore;
    }
}
```

**5. SubscriptionStatus 상태 머신** — 구독 생명주기:

```java
// domain/account/subscription/model/SubscriptionStatus.java
public enum SubscriptionStatus {
    PENDING(Set.of("ACTIVE", "CANCELLED")),
    ACTIVE(Set.of("CANCELLED", "EXPIRED")),
    CANCELLED(Set.of("ACTIVE")),  // 재활성화 가능
    EXPIRED(Set.of("ACTIVE"));    // 재구독 가능

    public void validateTransitionTo(SubscriptionStatus target) {
        if (!allowedTransitions.contains(target.name())) {
            throw new IllegalStateException(
                String.format("Invalid subscription status transition: %s -> %s",
                    this.name(), target.name()));
        }
    }
}
```

**7개 Domain Event**: VesselStatusChangedEvent, RiskScoreChangedEvent, SanctionedVesselDetectedEvent, SanctionActivatedEvent, SubscriptionActivatedEvent, SubscriptionCancelledEvent, PaymentCompletedEvent

### 결과 (Before/After 수치 테이블)

| 지표 | Before | After |
|------|--------|-------|
| 도메인 이벤트 수 | 0개 | 7개 |
| 상태 머신 적용 Entity | 0개 | 2개 (VesselStatus, SubscriptionStatus) |
| 잘못된 상태 전이 방지 | 불가 (아무 전이나 허용) | validateTransitionTo()로 즉시 차단 |
| 사이드 이펙트 결합도 | 높음 (Service에서 직접 호출) | 낮음 (EventListener로 분리) |
| 감사 추적 | 불가 (이력 없음) | 이벤트 ID + 발생 시각 + 이전/이후 상태 기록 |
| 새 사이드 이펙트 추가 | 기존 Service 코드 수정 필요 | 새 EventListener 추가만 (OCP 준수) |
| domain 모듈 Spring 의존성 | 해당 없음 | 순수 POJO 유지 (Spring 의존 없음) |
