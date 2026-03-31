# AnchorIQ — 핵심 시퀀스 다이어그램

> 복잡한 흐름의 **순서와 방향**만 가이드 — 클래스명/메서드명은 구현 시 자연스럽게 정할 것
> 다이어그램의 핵심은 "어떤 컴포넌트가 어떤 순서로 통신하는가"

---

## 목차
- [1. 결제 흐름](#1-결제-흐름)
- [2. AIS 데이터 파이프라인](#2-ais-데이터-파이프라인)
- [3. 리스크 알림 전체 흐름](#3-리스크-알림-전체-흐름)
- [4. 로그인 + JWT 갱신](#4-로그인--jwt-갱신)
- [5. 자연어 AI 질의](#5-자연어-ai-질의)
- [6. What-if 시뮬레이션](#6-what-if-시뮬레이션)

---

## 1. 결제 흐름

> Tier 1 강한 일관성 + 보상 트랜잭션

### 정상 흐름

```
[React]                     [SubscriptionController]       [PaymentApplicationService]       [PaymentGateway]         [PostgreSQL]
  │                              │                              │                              │                      │
  ├─ POST /api/payments/subscribe ─▶│                              │                              │                      │
  │  {plan: PRO, currency: USD}  │                              │                              │                      │
  │                              ├─ processPayment() ──────────▶│                              │                      │
  │                              │                              │                              │                      │
  │                              │                              ├─ resolve(USD) ──────────────▶│ StripeGateway         │
  │                              │                              │                              │                      │
  │                              │                              │  ① 외부 결제 (트랜잭션 밖)     │                      │
  │                              │                              ├─ charge(request) ───────────▶│                      │
  │                              │                              │◀─ PaymentGatewayResult ──────┤                      │
  │                              │                              │                              │                      │
  │                              │                              │  ② DB 저장 (@Transactional)   │                      │
  │                              │                              ├─ Payment.create() ──────────────────────────────────▶│ INSERT payment
  │                              │                              ├─ subscription.activate(PRO) ────────────────────────▶│ UPDATE subscription
  │                              │                              │                              │                      │
  │                              │◀─ PaymentResult.success() ──┤                              │                      │
  │◀─ 200 OK ────────────────────┤                              │                              │                      │
```

### 실패 흐름 — DB 저장 실패 시 환불

```
[PaymentApplicationService]       [StripeGateway]              [PostgreSQL]
  │                              │                              │
  │  ① 외부 결제 성공             │                              │
  ├─ charge() ──────────────────▶│                              │
  │◀─ 성공 ─────────────────────┤                              │
  │                              │                              │
  │  ② DB 저장 실패!              │                              │
  ├─ save(payment) ─────────────────────────────────────────────▶│ ❌ 예외 발생
  │                              │                              │
  │  ③ 보상: 환불                 │                              │
  ├─ refund(paymentId) ─────────▶│                              │
  │◀─ 환불 완료 ────────────────┤                              │
  │                              │                              │
  │  throw PaymentProcessingException("DB 실패, 환불 완료")      │
```

---

## 2. AIS 데이터 파이프라인

> Tier 2 최종 일관성 — Kafka 기반 독립 처리

```
[AISstream WebSocket]      [AisStreamClient]      [Kafka: ais-positions]
  │                          │                      │
  │ ── AIS JSON 메시지 ─────▶│                      │
  │                          ├─ parse() ───────────▶│ produce(key=mmsi)
  │                          │                      │
  │ ── AIS JSON 메시지 ─────▶│                      │
  │                          ├─ parse() ───────────▶│ produce(key=mmsi)
  │ (초당 수백 건)             │                      │
  │                          │                      │
                                                    │
                            ┌───────────────────────┼───────────────────────┐
                            │                       │                       │
                    [AisRedisConsumer]      [AisNeo4jConsumer]    [AisAnomalyDetectorConsumer]
                            │                       │                       │
                            │                       │                       │
                     ┌──────▼──────┐        ┌───────▼───────┐      ┌───────▼───────┐
                     │ Redis GEO   │        │ Neo4j         │      │ AI Engine     │
                     │ GEOADD      │        │ Vessel.status │      │ 이상 탐지      │
                     │ vessels:    │        │ = SAILING     │      │               │
                     │ positions   │        │ / ANCHORED    │      │ AIS 끊김?     │
                     │ TTL: 30초   │        │               │      │ 항로 이탈?    │
                     └─────────────┘        └───────────────┘      │ 속도 이상?    │
                                                                    └───────┬───────┘
                                                                            │
                                                                    이상 발견 시
                                                                            │
                                                                    ┌───────▼───────┐
                                                                    │ Kafka:         │
                                                                    │ risk-alerts    │
                                                                    │ produce()      │
                                                                    └────────────────┘
```

### 재시도 + DLT 흐름

```
[Kafka: ais-positions]      [AisRedisConsumer]      [Redis]        [DLT: ais-positions.DLT]
  │                          │                      │               │
  ├─ message ───────────────▶│                      │               │
  │                          ├─ GEOADD ────────────▶│ ❌ 실패       │
  │                          │                      │               │
  │                          │  1초 후 재시도 (1/3)   │               │
  │                          ├─ GEOADD ────────────▶│ ❌ 실패       │
  │                          │                      │               │
  │                          │  1초 후 재시도 (2/3)   │               │
  │                          ├─ GEOADD ────────────▶│ ❌ 실패       │
  │                          │                      │               │
  │                          │  1초 후 재시도 (3/3)   │               │
  │                          ├─ GEOADD ────────────▶│ ❌ 최종 실패   │
  │                          │                      │               │
  │                          ├─ DLT로 이동 ─────────────────────────▶│ 메시지 보관 (30일)
  │                          │  다음 메시지 계속 처리  │               │
```

---

## 3. 리스크 알림 전체 흐름

> 이벤트 발생 → AI 판단 → 알림 발송 → 프론트 실시간 표시

### 예시: "태풍 접근 + 항만 혼잡 → 대체 항만 추천"

```
[Open-Meteo]      [WeatherCollector]     [Kafka: weather-events]     [WeatherRiskConsumer]
  │                  │                      │                          │
  │◀─ GET /forecast ─┤                      │                          │
  │── 태풍 데이터 ───▶│                      │                          │
  │                  ├─ produce() ─────────▶│                          │
  │                  │                      ├─ consume() ─────────────▶│
  │                  │                      │                          │
  │                  │                      │              ┌───────────┤
  │                  │                      │              │           │
  │                  │                      │         [Neo4j]     [Redis]
  │                  │                      │              │           │
  │                  │                      │         태풍 영향   항만 혼잡도
  │                  │                      │         해역 조회   조회
  │                  │                      │              │           │
  │                  │                      │              └─────┬─────┘
  │                  │                      │                    │
  │                  │                      │         [SupplyChainRiskService]
  │                  │                      │                    │
  │                  │                      │         태풍 + 혼잡도 + 선박 위치 조합
  │                  │                      │         → RiskLevel: HIGH
  │                  │                      │         → 추천: "광양항 우회"
  │                  │                      │                    │
  │                  │                      │         [Kafka: risk-alerts]
  │                  │                      │              produce()
  │                  │                      │                    │
                                                     ┌──────────┼──────────┐
                                                     │          │          │
                                              [N8nConsumer] [ESConsumer] [WSConsumer]
                                                     │          │          │
                                                     ▼          ▼          ▼
                                                [n8n 웹훅]  [ES 저장]  [WebSocket]
                                                     │                     │
                                                     ▼                     ▼
                                              [Slack 알림]          [React 대시보드]
                                              [Email 알림]          실시간 알림 표시
```

---

## 4. 로그인 + JWT 갱신

```
[React]                    [AuthController]         [AuthApplicationService]    [PostgreSQL]     [Redis]
  │                          │                          │                        │              │
  │ ── POST /api/auth/login ─▶│                          │                        │              │
  │    {email, password}     │                          │                        │              │
  │                          ├─ login() ───────────────▶│                        │              │
  │                          │                          ├─ findByEmail() ────────▶│              │
  │                          │                          │◀─ User ────────────────┤              │
  │                          │                          ├─ BCrypt.matches() ──── 비밀번호 검증    │
  │                          │                          ├─ JwtTokenProvider      │              │
  │                          │                          │   .generateAccess()    │              │
  │                          │                          │   .generateRefresh()   │              │
  │                          │                          ├─ refreshToken 저장 ───────────────────▶│
  │                          │◀─ {accessToken, refreshToken} ─┤                  │              │
  │◀─ 200 OK ────────────────┤                          │                        │              │
  │                          │                          │                        │              │
  │ (15분 후 accessToken 만료) │                          │                        │              │
  │                          │                          │                        │              │
  │ ── GET /api/vessels ─────▶│                          │                        │              │
  │    Authorization: Bearer  │                          │                        │              │
  │◀─ 401 Unauthorized ──────┤                          │                        │              │
  │                          │                          │                        │              │
  │ ── POST /api/auth/refresh ▶│                          │                        │              │
  │    {refreshToken}        │                          │                        │              │
  │                          ├─ refresh() ─────────────▶│                        │              │
  │                          │                          ├─ refreshToken 검증 ──────────────────▶│
  │                          │                          │◀─ 유효 ──────────────────────────────┤
  │                          │                          ├─ 새 accessToken 발급    │              │
  │                          │◀─ {newAccessToken} ──────┤                        │              │
  │◀─ 200 OK ────────────────┤                          │                        │              │
  │                          │                          │                        │              │
  │ ── GET /api/vessels ─────▶│ (새 토큰으로 재요청)       │                        │              │
  │◀─ 200 OK + 데이터 ────────┤                          │                        │              │
```

### 프론트 Axios 인터셉터

```
[Axios Interceptor]
  │
  ├─ 요청 시: Authorization 헤더에 accessToken 자동 첨부
  │
  ├─ 401 응답 시:
  │   ├─ refreshToken으로 POST /api/auth/refresh 자동 호출
  │   ├─ 새 accessToken 저장
  │   └─ 원래 요청 재시도
  │
  └─ refreshToken도 만료 시:
      └─ 로그인 페이지로 리다이렉트
```

---

## 5. 자연어 AI 질의

> "호르무즈 해협 근처 제재 연관 선박?"

```
[React: QueryBar]      [AiQueryController]      [AiQueryApplicationService]     [OpenClaw]        [Neo4j]         [Redis]
  │                      │                          │                            │               │              │
  │ ── POST /api/ai/query ▶│                          │                            │               │              │
  │    {query: "호르무즈   │                          │                            │               │              │
  │     해협 근처 제재      │                          │                            │               │              │
  │     연관 선박?"}       │                          │                            │               │              │
  │                      ├─ handleQuery() ─────────▶│                            │               │              │
  │                      │                          │                            │               │              │
  │                      │                          │  ① 플랜 체크                │               │              │
  │                      │                          ├─ subscriptionService       │               │              │
  │                      │                          │   .canUseFeature()         │               │              │
  │                      │                          │                            │               │              │
  │                      │                          │  ② API 사용량 체크          │               │              │
  │                      │                          ├─ INCR api:usage:{userId} ──────────────────────────────────▶│
  │                      │                          │◀─ count: 3 (5건 이하 OK) ──────────────────────────────────┤
  │                      │                          │                            │               │              │
  │                      │                          │  ③ 캐시 확인                │               │              │
  │                      │                          ├─ GET ai:result:{queryHash} ────────────────────────────────▶│
  │                      │                          │◀─ null (캐시 미스) ─────────────────────────────────────────┤
  │                      │                          │                            │               │              │
  │                      │                          │  ④ 자연어 → Cypher 변환     │               │              │
  │                      │                          ├─ translateToCypher() ─────▶│               │              │
  │                      │                          │  "이 질문을 Neo4j Cypher로  │               │              │
  │                      │                          │   변환해줘"                 │               │              │
  │                      │                          │◀─ Cypher 쿼리 ────────────┤               │              │
  │                      │                          │                            │               │              │
  │                      │                          │  ⑤ Neo4j 쿼리 실행          │               │              │
  │                      │                          ├─ MATCH (v:Vessel)... ──────────────────────▶│              │
  │                      │                          │◀─ 결과: 선박 2척 ──────────────────────────┤              │
  │                      │                          │                            │               │              │
  │                      │                          │  ⑥ Redis GEO 위치 보강      │               │              │
  │                      │                          ├─ GEOPOS vessels:positions ─────────────────────────────────▶│
  │                      │                          │◀─ 좌표 데이터 ─────────────────────────────────────────────┤
  │                      │                          │                            │               │              │
  │                      │                          │  ⑦ AI 자연어 응답 생성       │               │              │
  │                      │                          ├─ generateResponse() ──────▶│               │              │
  │                      │                          │  "결과를 자연어로 정리해줘"   │               │              │
  │                      │                          │◀─ 자연어 응답 ──────────────┤               │              │
  │                      │                          │                            │               │              │
  │                      │                          │  ⑧ 결과 캐싱 (TTL 5분)      │               │              │
  │                      │                          ├─ SET ai:result:{hash} ─────────────────────────────────────▶│
  │                      │                          │                            │               │              │
  │                      │                          │  ⑨ ES 로그 저장 (비동기)     │               │              │
  │                      │                          ├─ @Async → ES ai-decisions  │               │              │
  │                      │                          │                            │               │              │
  │                      │◀─ AiQueryResponse ───────┤                            │               │              │
  │◀─ 200 OK ────────────┤                          │                            │               │              │
  │  {answer: "현재 호르무즈│                          │                            │               │              │
  │   해협 반경 50km 내에   │                          │                            │               │              │
  │   제재 연관 선박 2척... │                          │                            │               │              │
  │   vessels: [...]}     │                          │                            │               │              │
```

---

## 6. What-if 시뮬레이션

> "수에즈 운하 3일 봉쇄 시 영향?"

```
[React]           [AiWhatIfController]    [WhatIfService]           [OpenClaw]        [Neo4j]
  │                  │                      │                        │               │
  │ ── POST ────────▶│                      │                        │               │
  │  /api/ai/whatif  │                      │                        │               │
  │  {scenario:      │                      │                        │               │
  │   "수에즈 3일     ├─ simulate() ────────▶│                        │               │
  │    봉쇄"}        │                      │                        │               │
  │                  │                      │  ① 수에즈 통과 선박 조회  │               │
  │                  │                      ├─ MATCH (r:Route)       │               │
  │                  │                      │  -[:PASSES_THROUGH]->  │               │
  │                  │                      │  (cp {name:'Suez'})    │               │
  │                  │                      │  <-[:SAILING_ON]-      │               │
  │                  │                      │  (v:Vessel) ───────────────────────────▶│
  │                  │                      │◀─ 영향 선박 150척 ─────────────────────┤
  │                  │                      │                        │               │
  │                  │                      │  ② 대체 항로 분석        │               │
  │                  │                      ├─ 아프리카 희망봉 우회    │               │
  │                  │                      │  추가 거리/시간/비용 계산 │               │
  │                  │                      │                        │               │
  │                  │                      │  ③ AI 영향 분석          │               │
  │                  │                      ├─ analyzeImpact() ─────▶│               │
  │                  │                      │  "150척 영향, 대체 항로  │               │
  │                  │                      │   비용, 예상 지연을      │               │
  │                  │                      │   분석해줘"              │               │
  │                  │                      │◀─ 분석 결과 ────────────┤               │
  │                  │                      │                        │               │
  │                  │                      │  ④ 결과 저장 (PostgreSQL) │               │
  │                  │                      ├─ WhatIfResult 저장      │               │
  │                  │                      │                        │               │
  │                  │◀─ WhatIfResponse ────┤                        │               │
  │◀─ 200 OK ────────┤                      │                        │               │
  │  {impact:        │                      │                        │               │
  │   affectedVessels: 150,                 │                        │               │
  │   estimatedDelay: "5~14일",             │                        │               │
  │   additionalCost: "$1.2M/척",           │                        │               │
  │   alternativeRoute: "Cape of Good Hope",│                        │               │
  │   recommendations: [...]}               │                        │               │
```
