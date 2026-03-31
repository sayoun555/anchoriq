## Value Object + 일급 컬렉션 적용: String/int raw 타입 → 9개 VO + 4개 일급 컬렉션으로 타입 안전성 확보

### 배경 및 문제정의
- 상황: 해운 도메인에는 IMO 번호(7자리), MMSI 번호(9자리), UN/LOCODE(5자리), ISO 국가 코드(2자리), 국기(Flag) 등 고유한 식별 체계가 존재한다. 초기 설계에서는 이 모든 값을 `String`이나 `int`로 표현했다.
- 문제:
  - **타입 안전성 없음**: `String imo`와 `String mmsi`가 같은 타입이므로, IMO 필드에 MMSI 값을 넣어도 컴파일이 통과한다. 런타임에 잘못된 데이터가 DB에 저장되어야 비로소 발견된다.
  - **검증 로직 분산**: IMO 7자리 검증을 Controller, Service, Repository 등 여러 곳에서 중복 수행했다. 한 곳의 검증을 빠뜨리면 잘못된 데이터가 유입된다.
  - **비즈니스 로직 누출**: `List<Chokepoint>`를 직접 다루면서 "고위험 초크포인트가 있는지" 같은 판단 로직이 Service 레이어에 흩어져 있었다. 컬렉션 자체가 비즈니스 의미를 가지지 못했다.

### 기술 선정 (대안 비교 테이블)

| 항목 | String 유지 + 검증 메서드 | Value Object 포장 |
|------|--------------------------|-------------------|
| 컴파일 타임 타입 체크 | X (String끼리 대입 가능) | O (Imo와 Mmsi는 다른 타입) |
| 검증 위치 | 분산 (호출부마다 수동 검증) | 중앙화 (VO 생성자에서 1회) |
| equals/hashCode | 수동 구현 필요 | VO 내부에서 값 기반 비교 보장 |
| 불변성 | 보장 안 됨 (String은 불변이지만 재할당 가능) | final 필드로 불변 보장 |
| 코드 가독성 | `String imo` (도메인 의미 불명확) | `Imo imo` (도메인 의미 명확) |
| 초기 코드량 | 적음 | VO 클래스 파일 추가 |

**선택: Value Object 포장**. DDD에서 VO는 "값이 같으면 같은 객체"라는 개념으로, 도메인 모델의 표현력을 높인다. 컴파일 타임에 잘못된 대입을 잡아내는 것은 런타임 에러보다 100배 저렴하다.

### 분석 / CS 원리

**Primitive Obsession (원시 타입 집착) 안티패턴**

Martin Fowler는 "원시 타입으로 도메인 개념을 표현하는 것은 코드 냄새"라고 지적한다. 해운 도메인에서 IMO와 MMSI는 완전히 다른 개념이지만, 둘 다 `String`으로 표현하면 타입 시스템의 보호를 받지 못한다.

```java
// Before: 컴파일러가 잘못된 대입을 잡지 못한다
void updateVessel(String imo, String mmsi, String flag) {
    // mmsi를 imo 위치에 넣어도 컴파일 통과 → 런타임 오류
}

// After: 타입이 다르므로 컴파일 에러
void updateVessel(Imo imo, Mmsi mmsi, Flag flag) {
    // Mmsi를 Imo 위치에 넣으면 컴파일 에러 → 즉시 발견
}
```

**일급 컬렉션 (First-Class Collection)**

컬렉션을 클래스로 감싸면:
1. **비즈니스 규칙 캡슐화**: `chokepoints.hasHighRisk()` — 판단 로직이 컬렉션 내부에 있다.
2. **불변성 보장**: `Collections.unmodifiableList()`로 외부에서 수정 불가.
3. **상태와 행위 응집**: 컬렉션 관련 로직이 한 곳에 모인다.

### 솔루션 (핵심 코드 블록)

**1. Imo Value Object** — 생성 시 검증, 값 기반 동등성:

```java
// domain/maritime/vessel/model/Imo.java
public class Imo {

    private final String value;

    private Imo(String value) {
        validate(value);
        this.value = value;
    }

    public static Imo of(String value) {
        return new Imo(value);
    }

    public String value() {
        return value;
    }

    private void validate(String value) {
        Objects.requireNonNull(value, "IMO must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("IMO must not be blank");
        }
        // IMO 번호는 반드시 7자리 숫자
        if (!value.matches("\\d{7}")) {
            throw new IllegalArgumentException(
                "IMO must be a 7-digit number, but was: " + value);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Imo imo = (Imo) o;
        return value.equals(imo.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
```

**2. Chokepoints 일급 컬렉션** — 비즈니스 로직 캡슐화:

```java
// domain/maritime/route/model/Chokepoints.java
public class Chokepoints {

    private final List<Chokepoint> values;

    public Chokepoints(List<Chokepoint> chokepoints) {
        this.values = chokepoints != null
                ? Collections.unmodifiableList(new ArrayList<>(chokepoints))
                : Collections.emptyList();
    }

    public static Chokepoints empty() {
        return new Chokepoints(Collections.emptyList());
    }

    // 비즈니스 판단 로직이 컬렉션 내부에 있다
    public boolean hasHighRisk() {
        return values.stream().anyMatch(Chokepoint::isHighRisk);
    }

    public int countHighRisk() {
        return (int) values.stream().filter(Chokepoint::isHighRisk).count();
    }

    public boolean passesThrough(String chokepointName) {
        return values.stream()
                .anyMatch(cp -> cp.getName().equalsIgnoreCase(chokepointName));
    }

    // 불변 컬렉션 — 추가 시 새 객체 반환
    public Chokepoints add(Chokepoint chokepoint) {
        Objects.requireNonNull(chokepoint, "Chokepoint must not be null");
        if (values.contains(chokepoint)) {
            return this;
        }
        List<Chokepoint> newValues = new ArrayList<>(values);
        newValues.add(chokepoint);
        return new Chokepoints(newValues);
    }
}
```

**3. Entity에서 VO 활용** — Vessel이 Imo, Mmsi, Flag를 타입으로 보유:

```java
// domain/maritime/vessel/model/Vessel.java
public class Vessel extends AggregateRoot {
    private Imo imo;      // String이 아닌 VO 타입
    private Mmsi mmsi;    // String이 아닌 VO 타입
    private Flag flag;    // String이 아닌 VO 타입

    public static Vessel create(String imo, String mmsi, String name,
                                String flag, VesselType type) {
        return builder()
                .imo(Imo.of(imo))     // 생성 시 자동 검증
                .mmsi(Mmsi.of(mmsi))  // 9자리 숫자 검증
                .flag(Flag.of(flag))  // ISO 국가코드 검증
                .name(name)
                .type(type)
                .build();
    }
}
```

**전체 VO 목록 (9개)**: Imo, Mmsi, Flag, Locode, IsoCountryCode, CongestionLevel, Coordinate, BaselineRatio, VesselCount

**전체 일급 컬렉션 목록 (4개)**: Chokepoints, RiskFactors, Features, Recommendations

### 결과 (Before/After 수치 테이블)

| 지표 | Before | After |
|------|--------|-------|
| 타입 안전성 | 없음 (String끼리 대입 가능) | 컴파일 타임 타입 체크 (Imo ≠ Mmsi) |
| 검증 로직 위치 | 3~4곳에 분산 | VO 생성자 1곳에 중앙화 |
| 잘못된 값 유입 | 런타임에 발견 | 생성 시점에 즉시 차단 (fail-fast) |
| Value Object 수 | 0개 | 9개 |
| 일급 컬렉션 수 | 0개 | 4개 |
| 비즈니스 로직 위치 | Service 레이어에 분산 | VO/일급 컬렉션 내부에 캡슐화 |
| 메서드 시그니처 가독성 | `(String, String, String)` | `(Imo, Mmsi, Flag)` — 도메인 의미 명확 |
