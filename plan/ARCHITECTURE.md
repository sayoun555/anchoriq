# AnchorIQ — 아키텍처 설계

> 온톨로지 모델 + 데이터 파이프라인 + 시스템 아키텍처 + DDD 설계

---

## 목차
- [아키텍처 패턴](#아키텍처-패턴)
- [시스템 아키텍처](#시스템-아키텍처)
- [온톨로지 모델](#온톨로지-모델)
- [데이터 파이프라인](#데이터-파이프라인)
- [모듈 구조](#모듈-구조)
- [DDD 설계](#ddd-설계)
- [AI 의사결정 시나리오](#ai-의사결정-시나리오)

---

## 아키텍처 패턴

> 2025-2026 실무 트렌드 기반 결정 — 실제 기업 사례 조사 후 확정

### 적용 패턴 요약

| 패턴 | 결정 | 근거 |
|------|------|------|
| **헥사고날 아키텍처** | ✅ 적용 | 도메인이 외부에 의존하지 않음 (Port/Adapter 원칙) |
| **모듈러 모놀리스** | ✅ 적용 | 솔로 개발에 MSA는 과함, 42% 기업이 MSA→모놀리스 회귀 중 |
| **DDD (풍부한 도메인)** | ✅ 적용 | 리스크 판단 로직이 엔티티 상태에 강하게 의존 |
| **이벤트 기반** | ✅ 적용 | Kafka 8개 토픽, 하나의 이벤트를 여러 Consumer가 독립 처리 |
| **자연스러운 CQRS** | ✅ 적용 | 쓰기=Kafka Consumer, 읽기=REST API (프레임워크 없이) |
| **MVC + WebClient** | ✅ 적용 | 동기 기본, 외부 호출만 비동기 (R2DBC 안 씀) |
| **Virtual Threads** | ✅ 적용 | Java 21, MVC 블로킹 문제 해소, WebFlux 전면 도입 불필요 |

### 헥사고날 아키텍처 (Ports & Adapters)

> 도메인이 중심, 외부는 교체 가능한 어댑터

```
                    [인바운드]                              [아웃바운드]
                        │                                      │
                  ┌─────▼─────┐                          ┌─────▼─────┐
                  │   Port    │                          │   Port    │
                  │ (인터페이스)│                          │ (인터페이스)│
                  └─────┬─────┘                          └─────┬─────┘
                        │                                      │
   [Controller] ────▶ [Application Service] ────▶ [Domain] ────▶ [Repository 인터페이스]
   (Adapter)            (오케스트레이션)         (비즈니스 로직)      (Port)
                                                                    │
                                                              [Infrastructure]
                                                              (Adapter = 구현체)
                                                                    │
                                                         ┌──────────┼──────────┐
                                                       Neo4j    PostgreSQL    Redis
```

우리 코드에서의 매핑:

```
Port (인터페이스):
  domain/repository/VesselRepository.java          ← 아웃바운드 포트
  domain/gateway/PaymentGateway.java               ← 아웃바운드 포트
  domain/service/SupplyChainRiskService.java       ← 도메인 서비스 인터페이스

Adapter (구현체):
  infrastructure/persistence/VesselRepositoryImpl   ← 아웃바운드 어댑터
  infrastructure/payment/StripeGateway.java         ← 아웃바운드 어댑터
  controller/VesselQueryController.java             ← 인바운드 어댑터

핵심: domain 패키지는 infrastructure에 절대 의존하지 않는다
```

> 폴더 이름은 기존 레이어드 구조 유지 (`domain/`, `infrastructure/`).
> 헥사고날 원칙만 적용하고, `port/`, `adapter/` 폴더로 나누지는 않는다.
> 이것이 실무에서 가장 많이 쓰는 "클린 레이어드 + 헥사고날 원칙" 조합.

### 모듈러 모놀리스

> 5개 모듈이 하나의 JAR로 빌드, 하나의 프로세스로 실행

```
왜 MSA가 아닌가:
- 솔로 개발에 MSA는 오버엔지니어링
- 인프라 비용: MSA는 모놀리스 대비 3.75~6배
- 아마존 Prime Video: MSA → 모놀리스 전환 → 인프라 비용 90% 절감
- 2025 CNCF 조사: 42% 기업이 MSA에서 모놀리스로 회귀

왜 모듈러인가:
- 모듈 경계가 명확 (core, api, collector, ai, automation)
- 모듈 간 의존성 규칙 강제 (core는 외부 의존 금지)
- 나중에 MSA로 쪼개야 할 때 모듈 단위로 분리 가능
```

면접 답변:
> "솔로 개발에 MSA는 오버엔지니어링입니다. 모듈 경계를 명확히 나눠서 필요 시 MSA 전환이 가능하게 설계했습니다. 실제로 아마존 Prime Video도 MSA에서 모놀리스로 전환하여 90% 비용 절감했습니다."

### MVC + WebClient + Virtual Threads

> 전면 WebFlux 아닌 하이브리드 — 실무 대세

```
동기 (Spring MVC):
  - REST API 엔드포인트 174개
  - DB 접근 (JPA, Spring Data Neo4j)
  - 결제 처리 후 DB 저장
  - Virtual Threads가 블로킹 I/O를 가상 스레드에서 처리 → 스레드 점유 문제 해소

비동기 (WebClient):
  - 외부 API 11개 호출 (Open-Meteo, GNews, EIA, Frankfurter, GDELT, OpenSanctions...)
  - OpenClaw AI 호출 (응답 최대 30초)
  - AIS WebSocket 수신

R2DBC 안 쓰는 이유:
  - JPA 못 씀 (ORM 기능 없음, 관계 매핑 없음)
  - JPA + R2DBC 동시 사용 불가 (Spring Boot 공식 이슈 미해결)
  - Virtual Threads가 DB 블로킹 문제를 이미 해소
  - 실무에서도 대부분 JPA/JDBC 유지
```

설정:
```yaml
spring:
  threads:
    virtual:
      enabled: true  # Java 21 Virtual Threads 활성화
```

면접 답변:
> "WebFlux 전면 도입 시 JPA를 못 쓰고 R2DBC로 전환해야 하는데, R2DBC는 ORM 기능이 없어 생산성이 떨어집니다. Java 21 Virtual Threads로 MVC에서도 블로킹 문제가 해소되므로, MVC + WebClient 하이브리드가 최적입니다. Netflix도 메인 서비스에서 WebFlux를 쓰지 않습니다."

### 자연스러운 CQRS

> 프레임워크(Axon 등) 없이 모듈 구조로 자연스럽게 분리

```
쓰기 경로: 데이터 수집 → Kafka → Consumer → Neo4j/Redis/ES 저장
          (collector, automation 모듈)

읽기 경로: REST API 요청 → Service → Neo4j/Redis/ES 조회 → 응답
          (api 모듈)

이미 모듈이 분리되어 있어서 사실상 CQRS 구조
별도 프레임워크 불필요
```

---

## 시스템 아키텍처

```
[데이터 소스 — 11개 API]
 AIS WebSocket / Open-Meteo / UN 제재 / GNews / EIA / Frankfurter
 GDELT / Marine Regions / Natural Earth / UN/LOCODE / UNCTAD(크롤링)
                    ↓
              Kafka (이벤트 스트림)
                    ↓
         Spring Boot (Ontology Engine)
          ├── anchoriq-collector  (수집/정제)
          ├── anchoriq-core       (도메인/온톨로지)
          ├── anchoriq-ai         (리스크 판단)
          ├── anchoriq-api        (REST API)
          └── anchoriq-automation (n8n 연동)
                    ↓
    ┌───────────────┼───────────────┐
  Neo4j        PostgreSQL      Elasticsearch
 (온톨로지)     (유저/결제)      (뉴스/로그)
                    │
                  Redis
            (위치 캐싱/GEO)
                    ↓
              n8n (액션 자동화)
          Slack / Email / Webhook
                    ↓
          React 대시보드
    지도 + 그래프 뷰 + 타임라인 + 워룸
```

---

## 온톨로지 모델

> 세상을 객체(Entity)와 관계(Relationship)로 모델링
> 팔란티어 Foundry의 Object + Link 구조와 동일한 개념

```
Vessel ──OWNED_BY──▶ Company ──REGISTERED_IN──▶ Country
  │                     │                          │
  ├──DOCKED_AT──▶ Port  ├──SANCTIONED_BY──▶ UN     ├──GEOPOLITICAL_RISK
  │                     │                          ├──CONFLICT_ZONE
  ▼                     ▼                          │
Route ──PASSES_THROUGH──▶ Chokepoint (수에즈, 호르무즈, 말라카, 바브엘만데브, 대만해협, 파나마)
  │
  ▼
SeaZone ──HAS_WEATHER──▶ Condition
         ──JURISDICTION──▶ EEZ (배타적경제수역)
```

### Neo4j Cypher 예시 (4홉)

```cypher
-- "제재국 소유 선박 중 호르무즈 해협 접근 중인 것"
MATCH (v:Vessel)-[:OWNED_BY]->(c:Company)-[:REGISTERED_IN]->(co:Country)-[:SANCTIONED_BY]->(s:Sanction)
WHERE v.type = 'TANKER'
WITH v
MATCH (v)-[:SAILING_ON]->(r:Route)-[:PASSES_THROUGH]->(cp:Chokepoint {name: 'Hormuz'})
RETURN v.name, v.imo, v.flag
```

---

## 데이터 파이프라인 (Foundry 스타일)

> Raw → Clean → Ontology → Logic → Action 5단계

```
[Raw Layer]        AIS신호, 뉴스, 날씨, 제재목록, 유가, 환율
                   → 원본 그대로 Kafka에 적재
     ↓ 수집/정제
[Clean Layer]      정규화된 엔티티 매핑
                   → 필드 통일, 좌표 변환, 중복 제거
     ↓ 융합
[Ontology Layer]   Neo4j 그래프 — 엔티티 간 관계 자동 연결
                   → Vessel-Port, Vessel-Route, Route-Chokepoint 관계 생성
     ↓ 분석
[Logic Layer]      리스크 스코어링 + 이상 탐지 + LLM 추론
                   → Domain Service + OpenClaw AI
     ↓ 행동
[Action Layer]     n8n 워크플로우 자동 실행
                   → Slack 알림, Email, Webhook 트리거
```

---

## 모듈 구조

> 모노레포 + Gradle 멀티 모듈

```
/anchoriq/
  /backend/
    /anchoriq-core/        → 도메인 엔티티, VO, Aggregate Root, Domain Service, Repository 인터페이스
    /anchoriq-api/         → REST Controller, DTO, 입력 검증
    /anchoriq-collector/   → 11개 API 수집기, UNCTAD 크롤러, Kafka Producer
    /anchoriq-ai/          → OpenClaw 연동, 리스크 판단, 자연어 질의
    /anchoriq-automation/  → n8n 연동, 이벤트 → 액션 처리
  /frontend/               → React 대시보드
  /infra/                  → Docker Compose, n8n 설정
  /docs/                   → 설계 문서, 트러블슈팅 기록
```

### 모듈 간 의존성

```
anchoriq-api → anchoriq-core, anchoriq-ai
anchoriq-collector → anchoriq-core
anchoriq-ai → anchoriq-core
anchoriq-automation → anchoriq-core, anchoriq-ai
```

`anchoriq-core`는 다른 모듈에 의존하지 않음 (DIP 원칙).

---

## DDD 설계

### 설계 원칙

> **풍부한 도메인 모델** — Entity가 비즈니스 로직을 가짐
> 빈약한 도메인(Service가 다 하는 구조)이 아닌, Entity/VO에 판단 로직을 넣는 구조

#### 왜 풍부한 도메인인가?

```
빈약한 도메인이 맞는 경우: 단순 CRUD (게시판, 투두리스트)
풍부한 도메인이 맞는 경우: 비즈니스 규칙이 복잡한 경우 ← AnchorIQ
```

리스크 판단 규칙이 엔티티 상태에 강하게 의존하기 때문에,
로직을 엔티티에 두는 게 응집도가 높고 테스트도 쉽다.

### 레이어 구조 (각 모듈 내부)

```
api/              → Controller (입력 검증, 응답 변환)
application/      → Application Service (오케스트레이션만, 비즈니스 로직 X)
domain/
  ├── model/      → Entity, VO, Aggregate Root (비즈니스 로직 보유)
  ├── service/    → Domain Service (여러 Aggregate 걸치는 로직)
  ├── repository/ → Repository 인터페이스 (구현은 infrastructure)
  └── event/      → Domain Event
infrastructure/   → 외부 연동 (DB 구현체, API 클라이언트, Kafka)
```

### 풍부한 도메인 예시

```java
// Entity가 비즈니스 로직을 가짐
class Vessel {
    RiskLevel evaluateRisk(Chokepoint chokepoint, SanctionList sanctions) {
        if (this.isRegisteredIn(sanctions.getCountries())
            && this.isApproaching(chokepoint)) {
            return this.escalateRisk();
        }
        return this.currentRisk;
    }
}

// Domain Service — 여러 Aggregate 조합 판단
class SupplyChainRiskDomainService {
    RiskAssessment assessRouteRisk(Vessel vessel, Route route, WeatherCondition weather) {
        // Vessel + Route + Weather 3개 Aggregate 조합하여 리스크 판단
    }
}

// Application Service — 오케스트레이션만
class RiskApplicationService {
    RiskAssessment evaluateVesselRisk(Long vesselId) {
        Vessel vessel = vesselRepository.findById(vesselId);
        Route route = routeRepository.findCurrentRoute(vessel);
        WeatherCondition weather = weatherRepository.findByZone(route.getCurrentZone());
        return supplyChainRiskDomainService.assessRouteRisk(vessel, route, weather);
    }
}
```

### Aggregate Root

| Aggregate Root | 포함 엔티티/VO | 역할 |
|---------------|---------------|------|
| **Vessel** | 선박 정보, 위치 이력, 리스크 레벨, Watchlist | 온톨로지 중심 엔티티, 리스크 자체 판단 |
| **Route** | 항로, 경유 초크포인트 목록 | 항로 단위 리스크 계산 |
| **Port** | 항만 정보, 혼잡도 | 항만 상태 관리 |
| **RiskAssessment** | AI 판단 결과, 근거, 추천 액션 | 판단 기록 (불변) |
| **AnomalyDetection** | AIS 소실, 항로 이탈, 속도 이상, 다크 쉽 | 이상 탐지 기록 |
| **User** | 계정, 구독, API 사용량, 즐겨찾기 | 인증/결제 단위 |
| **Payment** | 결제 이력, 구독 상태 | 결제 트랜잭션 단위 |
| **Workflow** | n8n 워크플로우 설정, 트리거 조건, 실행 이력 | 자동화 단위 |
| **NotificationRule** | 알림 규칙, 채널 설정 | 알림 조건 단위 |
| **AuditLog** | 사용자 활동 이력 (로그인, 조회, 액션) | 감사 추적 (불변) |
| **ApiKey** | 외부 API 키, 사용량 | Enterprise API 접근 관리 |

### Domain Service

| Domain Service | 역할 | 왜 Domain Service인가 |
|---------------|------|---------------------|
| `SupplyChainRiskDomainService` | Vessel + Route + Weather + Sanction 조합 리스크 판단 | 여러 Aggregate 걸침 |
| `SanctionScreeningDomainService` | 선박/회사 제재 목록 대조 | Vessel + Sanction 걸침 |
| `RouteOptimizationDomainService` | 대체 항로/항만 추천 | Route + Port + Weather 걸침 |
| `AnomalyDetectionDomainService` | AIS 소실/항로 이탈/속도 이상 탐지 | Vessel + Route + SeaZone 걸침 |
| `RouteComparisonDomainService` | 항로 비교 (거리, 리스크, 비용) | Route + Chokepoint + Market 걸침 |
| `SubscriptionDomainService` | 플랜별 기능 제한 판단 | User + Subscription 걸침 |
| `ExportDomainService` | 리포트/CSV/PDF 생성 | 여러 도메인 데이터 조합 |

---

## AI 의사결정 시나리오

### 4홉으로 가능한 시나리오

| 시나리오 | 홉 경로 | AI 판단 |
|---------|---------|--------|
| 태풍 접근 + 항만 혼잡 | Vessel→Route→SeaZone→Weather | "부산항 혼잡 + 태풍 접근, 광양항 우회 추천" |
| 제재국 선박 탐지 | Vessel→Company→Country→Sanction | "이란 연관 선박이 호르무즈 진입 중" |
| AIS 신호 소실 | Vessel→마지막위치→EEZ→Country | "예멘 EEZ에서 AIS 끈 선박, 제재 회피 의심" |
| 유가 급등 + 항로 리스크 | Route→Chokepoint→리스크→유가 | "호르무즈 긴장, 아프리카 우회 시 추가 비용 $X" |

### AI 기능

| 기능 | 설명 |
|------|------|
| 자연어 질의 | "호르무즈 해협 근처 제재 연관 선박?" |
| 자동 브리핑 | 매일 아침 리스크 서머리 생성 |
| What-if 시뮬레이션 | "수에즈 3일 봉쇄 시 영향?" |
| 리스크 스코어링 | 엔티티별 0~100 리스크 점수 |
