## AIS Vessel 자동 생성 미구현 → Neo4j 0척에서 7,573척, 지도 표시 5,266척 달성

### 배경 및 문제정의
- **상황**: 데모 데이터 25척 삭제 후 Neo4j Vessel 노드 0개. AIS 스트림으로 수만 건의 선박 위치 데이터가 Kafka를 통해 유입되지만, 지도에 선박이 단 한 척도 표시되지 않음
- **문제 1 (핵심)**: `AisNeo4jConsumer`가 `vesselRepository.findByMmsi(mmsi).ifPresent(...)` 패턴 사용 — 기존 Vessel만 업데이트하고, Neo4j에 없는 MMSI는 전부 스킵. 새 Vessel 생성 로직 부재
- **문제 2 (IMO 검증)**: AIS 데이터의 대부분 선박은 IMO가 빈 문자열. `Imo.of("")` 호출 시 7자리 검증 실패로 `IllegalArgumentException` 발생하여 Consumer 전체가 중단
- **문제 3 (상태 전환)**: `Vessel.changeStatus()`가 도메인 불변식으로 상태 전환을 검증 (예: UNKNOWN → FISHING_ENGAGED 거부). AIS 원시 데이터는 어떤 상태든 올 수 있어 정상 데이터가 거부됨

### 기술 선정 (대안 비교 테이블)

| 항목 | Vessel 자동 생성 + nullable VO | 별도 AisVessel 엔티티 분리 | ACL에서 사전 필터링 |
|------|-------------------------------|--------------------------|-------------------|
| 구현 복잡도 | 낮음 (기존 모델 확장) | 높음 (새 엔티티 + 매핑) | 중간 |
| 데이터 정합성 | VO null 허용으로 유연 | 완전 분리로 안전 | 누락 위험 |
| 도메인 모델 영향 | 최소 (메서드 추가) | 없음 (별도 모델) | 없음 |
| 선택 이유 | **채택** — 기존 온톨로지 활용 | 중복 모델 관리 부담 | AIS 데이터 손실 |

### 분석
- **초기 판단**: AIS Kafka Consumer의 메시지 수신 자체가 안 되는 것으로 의심 → 로그 확인 결과 메시지는 정상 수신되나 `ifPresent` 분기에서 전부 스킵
- **실제 원인**: DDD Aggregate Root(Vessel)의 불변식이 외부 시스템(AIS) 데이터의 특성과 충돌. 세 가지 방어 로직이 동시에 데이터 유입을 차단:
  1. `findByMmsi().ifPresent()` — 존재하지 않으면 아무것도 안 함
  2. `Imo` Value Object — 빈 문자열을 유효하지 않은 IMO로 거부
  3. `changeStatus()` — 도메인이 허용하지 않는 상태 전환 거부
- **CS 원리**:
  - **DDD Conformist 패턴**: AIS는 외부 시스템(Upstream)이므로 내부 도메인이 외부 데이터 형식에 맞춰야 함. Aggregate Root 불변식을 외부 이벤트에 그대로 적용하면 데이터 유입이 차단됨
  - **Value Object nullable 설계**: 필수 VO(MMSI — 식별자)와 선택 VO(IMO, Flag — 보강 데이터)를 구분해야 함. AIS 데이터에서 IMO는 선택 필드
  - **External Event vs Domain Command**: Event Sourcing 관점에서 AIS 데이터는 외부 이벤트(External Event). 내부 도메인 규칙(상태 전환 검증)은 도메인 커맨드에만 적용해야 하며, 외부 이벤트는 있는 그대로 수용해야 함
  - **CQRS Write/Read 불일치**: Write Model(도메인 규칙 엄격)과 Read Model(지도 표시용 유연) 간의 요구사항 차이. AIS 데이터 수집은 Read Model 쪽에 가까움

### 솔루션

**1) AisNeo4jConsumer — findByMmsi 없으면 새 Vessel 생성**
```java
// Before: 기존 Vessel만 업데이트
vesselRepository.findByMmsi(mmsi).ifPresent(vessel -> {
    vessel.updatePosition(lat, lon);
});

// After: 없으면 자동 생성
Vessel vessel = vesselRepository.findByMmsi(mmsi)
    .orElseGet(() -> vesselRepository.save(
        Vessel.builder()
            .mmsi(Mmsi.of(mmsi))
            .imo(imo.isBlank() ? null : Imo.of(imo))       // nullable
            .flag(flag.isBlank() ? null : Flag.of(flag))    // nullable
            .name(shipName)
            .vesselType(VesselType.from(shipType))
            .status(NavigationStatus.fromAisCode(navStatus))
            .build()
    ));
vessel.updatePositionFromAis(lat, lon, heading, speed);
```

**2) Vessel 도메인 — 외부 이벤트용 상태 업데이트 메서드 추가**
```java
// 기존: 도메인 커맨드용 (상태 전환 검증)
public void changeStatus(NavigationStatus newStatus) {
    if (!this.status.canTransitionTo(newStatus)) {
        throw new InvalidStatusTransitionException(this.status, newStatus);
    }
    this.status = newStatus;
}

// 추가: 외부 이벤트용 (AIS 데이터 — 검증 없이 수용)
public void updateStatusFromAis(NavigationStatus aisStatus) {
    this.status = aisStatus;  // External Event는 있는 그대로 반영
}
```

**3) Neo4jNodeMapper — null-safe 매핑**
```java
// IMO, Flag가 null일 수 있으므로 매핑 시 null 체크
.imo(node.get("imo").isNull() ? null : Imo.of(node.get("imo").asString()))
.flag(node.get("flag").isNull() ? null : Flag.of(node.get("flag").asString()))
```

### 결과 (Before/After 수치 테이블)

| 지표 | Before | After |
|------|--------|-------|
| Neo4j Vessel 노드 | 0개 | 7,573개 |
| Map API 응답 선박 수 | 0척 | 5,266척 (Redis GEO 매칭분) |
| AIS Consumer 에러율 | IMO 검증 실패로 다수 중단 | 0% (null-safe 처리) |
| 상태 전환 거부 | 정상 AIS 데이터 거부 | 외부 이벤트 전량 수용 |
| 선박 상세 정보 완성도 | N/A | IMO 보유 약 30%, Flag 보유 약 60% |
