# AnchorIQ — 트랜잭션 설계

> 4개 DB(PostgreSQL, Neo4j, Redis, Elasticsearch) 환경에서의 트랜잭션 전략
> 핵심: 강한 일관성 / 최종 일관성 / 트랜잭션 불필요 — 3단계로 분류

---

## 목차
- [문제 정의](#문제-정의)
- [트랜잭션 3-Tier 전략](#트랜잭션-3-tier-전략)
- [Tier 1 — 강한 일관성](#tier-1--강한-일관성)
- [Tier 2 — 이벤트 기반 최종 일관성](#tier-2--이벤트-기반-최종-일관성)
- [Tier 3 — 트랜잭션 불필요](#tier-3--트랜잭션-불필요)
- [보상 트랜잭션](#보상-트랜잭션)
- [동시성 제어](#동시성-제어)
- [전체 트랜잭션 맵](#전체-트랜잭션-맵)

---

## 문제 정의

### 단일 DB면 간단함

```java
@Transactional
public void subscribe(Long userId, Plan plan) {
    paymentRepository.save(payment);           // 성공
    subscriptionRepository.save(subscription); // 실패 → 1번도 롤백
}
// PostgreSQL 하나 → @Transactional이면 끝
```

### 우리 프로젝트는 DB가 4개

```java
@Transactional  // ← PostgreSQL만 관리!
public void updateSanction(SanctionData data) {
    neo4jRepository.save(sanctionNode);      // Neo4j ← 범위 밖!
    redisTemplate.opsForSet().add(key, val); // Redis ← 범위 밖!
    esTemplate.save(logDocument);            // ES ← 범위 밖!
}
// Neo4j 실패해도 PostgreSQL은 이미 커밋 → 데이터 불일치
```

> `@Transactional`은 기본적으로 하나의 DB만 관리한다.
> 여러 DB에 걸치는 작업은 별도 전략이 필요하다.

---

## 트랜잭션 3-Tier 전략

| Tier | 대상 | 전략 | 일관성 |
|------|------|------|--------|
| **Tier 1** | 돈, 유저 데이터 | `@Transactional` + 보상 트랜잭션 | 강한 일관성 (즉시) |
| **Tier 2** | 온톨로지, 수집 데이터 | Kafka 이벤트 + DLT 재시도 | 최종 일관성 (수 초 지연 OK) |
| **Tier 3** | 캐시, 로그 | 실패 무시 또는 비동기 | 불필요 (실패해도 서비스 OK) |

### 왜 이렇게 나누는가

```
분산 트랜잭션(2PC)으로 전부 묶으면?
→ 모든 DB가 동시에 락 → 성능 최악
→ 하나라도 느리면 전체가 느려짐
→ 해운 리스크 플랫폼에서 선박 위치가 2초 늦는 건 OK
→ 하지만 결제가 반만 되는 건 절대 안 됨

결론: 중요도에 따라 전략을 나눈다
```

---

## Tier 1 — 강한 일관성

> 돈, 유저, 구독 → 절대 틀리면 안 되는 데이터
> PostgreSQL 단일 DB 내에서 `@Transactional`로 보장

### 대상 작업

| 작업 | DB | 방식 |
|------|-----|------|
| 회원가입 | PostgreSQL | `@Transactional` |
| 구독 변경 | PostgreSQL | `@Transactional` |
| 워크플로우 CRUD | PostgreSQL | `@Transactional` |
| 알림 규칙 CRUD | PostgreSQL | `@Transactional` |
| 관심 선박/즐겨찾기 | PostgreSQL | `@Transactional` |

### 핵심 규칙: 외부 API를 트랜잭션 안에 넣지 않는다

```
Bad:
  @Transactional 시작 → DB 커넥션 잡음
  → Stripe API 호출 → 3초 대기
  → 그 3초 동안 DB 커넥션 점유
  → 동시 요청 30개면 커넥션 풀 전부 잠김 → 장애

Good:
  1. Stripe 호출 (트랜잭션 밖)
  2. 성공하면 @Transactional로 DB 저장
  3. DB 실패하면 Stripe 환불 (보상 트랜잭션)
```

### 코드 예시

```java
// Application Service — 오케스트레이션
public class PaymentApplicationService {

    public PaymentResult processPayment(PaymentRequest request) {
        // 1. 외부 결제 (트랜잭션 밖)
        PaymentGateway gateway = paymentGatewayRouter.resolve(request.getCurrency());
        PaymentGatewayResult gatewayResult = gateway.charge(request);

        try {
            // 2. DB 저장 (트랜잭션)
            return savePaymentInTransaction(request, gatewayResult);
        } catch (Exception e) {
            // 3. 보상: 환불
            gateway.refund(gatewayResult.getPaymentId());
            throw new PaymentProcessingException("DB 저장 실패, 결제 환불 완료", e);
        }
    }

    @Transactional
    private PaymentResult savePaymentInTransaction(
            PaymentRequest request, PaymentGatewayResult result) {
        Payment payment = Payment.create(request, result);
        paymentRepository.save(payment);

        Subscription subscription = subscriptionRepository.findByUserId(request.getUserId());
        subscription.activate(request.getPlan());
        subscriptionRepository.save(subscription);

        return PaymentResult.success(payment);
    }
}
```

---

## Tier 2 — 이벤트 기반 최종 일관성

> 온톨로지 데이터 → 몇 초 지연은 OK, 결과적으로 맞으면 됨
> Kafka를 통해 각 DB가 독립적으로 처리

### 왜 최종 일관성으로 충분한가

```
AIS 선박 위치가 2초 늦게 Redis에 반영돼도?
→ 해운 리스크 판단에 영향 없음 (선박은 느림)

제재 목록이 3초 늦게 Neo4j에 반영돼도?
→ 3초 후에는 반영됨, 제재 선박은 즉시 움직이지 않음

뉴스가 5초 늦게 Elasticsearch에 들어가도?
→ 뉴스 검색이 5초 늦는 거지 서비스 장애 아님
```

### 동작 원리

```
AIS 데이터 들어옴:
  1. Producer → Kafka ais-positions 토픽에 발행 (여기까지가 보장)
  2. Consumer A → Redis GEO 저장 (독립)
  3. Consumer B → Neo4j 상태 업데이트 (독립)
  4. Consumer C → AI 이상 탐지 (독립)

  B가 실패하면?
  → Kafka에 메시지 남아있음
  → Consumer B 재시도 (최대 3회)
  → 3회 다 실패 → DLT (Dead Letter Topic)로 이동
  → 나중에 원인 분석 + 수동 재처리
  → 그 사이에도 A, C는 정상 동작
```

### 대상 작업

| 작업 | Producer | Consumer | 실패 시 |
|------|----------|----------|--------|
| AIS 위치 수집 | AIS WebSocket → Kafka | Redis, Neo4j, AI | Kafka 재시도 + DLT |
| 제재 목록 업데이트 | 수집기 → Kafka | Neo4j, Redis | Kafka 재시도 + DLT |
| 뉴스 수집 | GNews → Kafka | Elasticsearch, AI | Kafka 재시도 + DLT |
| 날씨 업데이트 | Open-Meteo → Kafka | Neo4j, AI | Kafka 재시도 + DLT |
| 유가/환율 | EIA/Frankfurter → Kafka | Redis, Neo4j | Kafka 재시도 + DLT |
| 항만 혼잡도 | UNCTAD 크롤러 → Kafka | Neo4j, Redis | Kafka 재시도 + DLT |
| 지정학 이벤트 | GDELT → Kafka | Elasticsearch, AI | Kafka 재시도 + DLT |
| 리스크 알림 | AI 엔진 → Kafka | n8n, ES, WebSocket | Kafka 재시도 + DLT |

### DLT (Dead Letter Topic) 처리

```
정상:   ais-positions → Consumer → Redis 저장 ✅
재시도1: ais-positions → Consumer → Redis 저장 ❌ (1초 후)
재시도2: ais-positions → Consumer → Redis 저장 ❌ (1초 후)
재시도3: ais-positions → Consumer → Redis 저장 ❌ (최종 실패)
DLT:    ais-positions.DLT → 메시지 보관 (30일) → 수동 분석
```

```java
@Bean
public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
    DeadLetterPublishingRecoverer recoverer =
        new DeadLetterPublishingRecoverer(kafkaTemplate);
    FixedBackOff backOff = new FixedBackOff(1000L, 3); // 1초 간격, 3번
    return new DefaultErrorHandler(recoverer, backOff);
}
```

---

## Tier 3 — 트랜잭션 불필요

> 실패해도 서비스에 영향 없는 작업

### 대상 작업

| 작업 | 실패하면? | 처리 |
|------|----------|------|
| Redis 캐시 저장 | 다음 요청에서 DB 직접 조회 (느리지만 동작) | 무시 |
| Redis 플랜 캐시 갱신 | DB에서 조회 fallback | 무시 |
| Elasticsearch 로그 저장 | 로그 유실, 서비스 장애 아님 | 무시 |
| Grafana 메트릭 | 모니터링 빈 구간 | 무시 |
| 감사 로그 | 감사 이력 빈 구간 | `@Async` 비동기 |

### 코드 예시 — Redis fallback

```java
public Vessel findVesselByImo(String imo) {
    // Redis에서 먼저 조회
    Vessel cached = redisTemplate.opsForValue().get("vessel:" + imo);
    if (cached != null) {
        return cached;
    }

    // 캐시 미스 → Neo4j에서 조회
    Vessel vessel = vesselRepository.findByImo(imo);

    // Redis에 캐싱 (실패해도 무시)
    try {
        redisTemplate.opsForValue().set("vessel:" + imo, vessel, Duration.ofMinutes(5));
    } catch (Exception e) {
        log.warn("Redis 캐시 저장 실패, 무시: {}", e.getMessage());
    }

    return vessel;
}
```

### 코드 예시 — 감사 로그 비동기

```java
@Async
@EventListener
public void handleAuditEvent(AuditEvent event) {
    // 실패해도 서비스에 영향 없음
    auditLogRepository.save(AuditLog.from(event));
}
```

---

## 보상 트랜잭션

> Tier 1에서 외부 API + DB가 걸칠 때

### 결제 시나리오

```
정상 흐름:
  1. Stripe 결제 성공 ✅
  2. DB Payment 저장 성공 ✅
  3. DB Subscription 업데이트 성공 ✅
  → 완료

실패 흐름 A — 외부 결제 실패:
  1. Stripe 결제 실패 ❌
  → 끝 (DB 작업 안 함)

실패 흐름 B — DB 저장 실패:
  1. Stripe 결제 성공 ✅
  2. DB 저장 실패 ❌
  → 보상: Stripe 환불 호출
  → 유저에게 "결제 처리 중 오류, 자동 환불됩니다" 응답

실패 흐름 C — 보상(환불)도 실패:
  1. Stripe 결제 성공 ✅
  2. DB 저장 실패 ❌
  3. Stripe 환불 실패 ❌
  → 실패 이벤트 Kafka 발행 → 수동 처리 큐
  → 관리자 알림 (Slack)
  → 유저에게 "고객센터 문의" 응답
```

### 구독 취소 시나리오

```
1. Stripe 구독 취소 API 호출
2. DB Subscription 상태 변경 (ACTIVE → CANCELLED)
3. Redis 플랜 캐시 갱신

2번 실패? → Stripe 구독 재활성화 (보상)
3번 실패? → Tier 3이라 무시 (다음 조회 시 DB에서 읽음)
```

---

## 동시성 제어

### 낙관적 락 (Optimistic Lock)

> 동시에 같은 데이터를 수정하면?

```
A: 구독 Pro로 변경 → 읽기(version=1) → 수정 → 저장(version=2) ✅
B: 구독 Free로 변경 → 읽기(version=1) → 수정 → 저장(version=2) ❌ OptimisticLockException
```

```java
@Entity
public class Subscription {
    @Version
    private Long version;  // JPA가 자동 관리, 저장 시 version 불일치면 예외
}
```

### 적용 대상

| 엔티티 | `@Version` | 이유 |
|--------|-----------|------|
| Subscription | ✅ | 동시 결제/변경 가능 |
| User | ✅ | 프로필 동시 수정 가능 |
| Workflow | ✅ | 워크플로우 동시 편집 가능 |
| NotificationRule | ✅ | 알림 규칙 동시 편집 가능 |
| Payment | ❌ | INSERT만, 수정 없음 |
| AuditLog | ❌ | INSERT만, 수정 없음 |

### Redis 분산 락 (필요 시)

> AI 질의 같은 무거운 작업이 동시에 중복 실행되는 걸 방지

```java
public AiResponse handleQuery(String query, Long userId) {
    String lockKey = "ai:query:lock:" + userId;
    Boolean acquired = redisTemplate.opsForValue()
        .setIfAbsent(lockKey, "locked", Duration.ofSeconds(30));

    if (!acquired) {
        throw new ConcurrentRequestException("이전 AI 질의가 처리 중입니다");
    }

    try {
        return aiService.query(query);
    } finally {
        redisTemplate.delete(lockKey);
    }
}
```

---

## 전체 트랜잭션 맵

| 작업 | 관련 DB | Tier | 전략 |
|------|---------|------|------|
| **회원가입** | PostgreSQL | 1 | `@Transactional` |
| **로그인** | PostgreSQL + Redis(토큰) | 1 + 3 | `@Transactional` + Redis 실패 무시 |
| **결제 처리** | Stripe/Toss + PostgreSQL | 1 | 보상 트랜잭션 (실패 시 환불) |
| **구독 변경** | PostgreSQL + Redis(캐시) | 1 + 3 | `@Transactional` + Redis 갱신 |
| **워크플로우 CRUD** | PostgreSQL | 1 | `@Transactional` |
| **알림 규칙 CRUD** | PostgreSQL | 1 | `@Transactional` |
| **관심 선박/즐겨찾기** | PostgreSQL | 1 | `@Transactional` |
| **AIS 데이터 수집** | Kafka → Redis + Neo4j + AI | 2 | Kafka 최종 일관성 |
| **제재 목록 업데이트** | Kafka → Neo4j + Redis | 2 | Kafka 최종 일관성 |
| **뉴스 저장** | Kafka → Elasticsearch | 2 | Kafka + DLT |
| **날씨 업데이트** | Kafka → Neo4j | 2 | Kafka 최종 일관성 |
| **유가/환율 업데이트** | Kafka → Redis + Neo4j | 2 | Kafka 최종 일관성 |
| **항만 혼잡도** | Kafka → Neo4j + Redis | 2 | Kafka 최종 일관성 |
| **지정학 이벤트** | Kafka → ES + AI | 2 | Kafka 최종 일관성 |
| **리스크 알림 발행** | Kafka → ES + n8n + WS | 2 | Kafka 최종 일관성 |
| **AI 질의** | Neo4j(읽기) + Redis(캐시) | 3 | 읽기 전용, 캐시 fallback |
| **리스크 스코어 조회** | Neo4j(읽기) + Redis(캐시) | 3 | 읽기 전용 |
| **감사 로그 저장** | PostgreSQL | 3 | `@Async` 비동기 |
| **내보내기 (PDF/CSV)** | 읽기 전용 | 3 | 트랜잭션 불필요 |

---

## 면접 포인트

> "트랜잭션 전략을 어떻게 설계했나요?"

### 한 마디 답변

> "데이터의 중요도에 따라 3-Tier로 분류했습니다.
> 결제/유저는 `@Transactional`로 강한 일관성을 보장하고,
> 온톨로지 데이터는 Kafka 이벤트 기반 최종 일관성으로 처리합니다.
> 외부 결제 API는 트랜잭션 밖에서 호출하고 보상 트랜잭션으로 실패를 처리합니다."

### 꼬리 질문 대비

| 질문 | 답변 |
|------|------|
| "왜 분산 트랜잭션(2PC) 안 썼나요?" | "4개 DB 전부 락 걸면 성능 최악. 해운 데이터의 몇 초 지연은 비즈니스에 영향 없어서 최종 일관성으로 충분합니다." |
| "최종 일관성에서 데이터 유실 가능성은?" | "Kafka DLT로 실패 메시지를 보관하고, 모니터링으로 DLT 쌓이는 걸 감지합니다." |
| "보상 트랜잭션에서 환불도 실패하면?" | "실패 이벤트를 Kafka에 발행하고 관리자에게 Slack 알림. 수동 처리 큐로 관리합니다." |
| "외부 API를 트랜잭션 밖에서 호출하는 이유?" | "트랜잭션 안에서 외부 호출하면 응답 대기 동안 DB 커넥션 점유. 커넥션 풀 고갈 위험." |
