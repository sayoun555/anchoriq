# AnchorIQ — 해운 공급망 리스크 탐지 자동화 플랫폼

선박/해운 데이터를 온톨로지(Neo4j)로 융합하고, AI가 리스크를 판단하여 n8n으로 자동 액션을 실행하는 팔란티어 Foundry 스타일 플랫폼입니다.

## 1. 프로젝트 소개

AnchorIQ는 11개 무료 API 데이터를 수집하여 Neo4j 온톨로지 그래프로 융합하고, AI(OpenAI Codex)가 공급망 리스크를 판단하여 n8n 워크플로우로 자동 알림/액션을 실행하는 해운 리스크 인텔리전스 플랫폼입니다.

### 핵심 기능
- 11개 외부 API(AIS/날씨/제재/뉴스/유가/환율/지정학/항만) 자동 수집 + Kafka 이벤트 스트림
- Neo4j 온톨로지 그래프 — 선박-회사-국가-항로-초크포인트 4홉 관계 탐색
- AI 리스크 스코어링 (0~100) + 자연어 질의 → Cypher 변환
- Kafka → n8n → Discord/Slack/Email 자동 알림 파이프라인
- 팔란티어 감성 대시보드 — 지도/그래프/워룸/타임라인 5개 뷰

### 담당 역할
- 전체 아키텍처 설계 및 구현 (백엔드 + 프론트엔드 + 인프라)
- 21개 설계 문서 작성 후 AI 기반 Vibe Coding으로 구현
- DDD(풍부한 도메인 모델) + 헥사고날 아키텍처 적용
- Kafka 이벤트 파이프라인 + n8n 자동화 연동
- Claude Code + MCP(Playwright, Chrome DevTools, Figma) 활용 개발 워크플로

### 기술 스택

| 레이어 | 기술 |
|------|------|
| Backend | Java 21, Spring Boot 3.4, Spring Security, Spring Data JPA/Neo4j/Redis, Spring Kafka |
| Frontend | React 19, TypeScript, Vite, Tailwind CSS, Mapbox/Leaflet, Cytoscape.js, Zustand |
| Database | Neo4j 5 (온톨로지), PostgreSQL 16 (유저/결제), Redis 7 (GEO/캐시), Elasticsearch 8 (검색) |
| Messaging | Kafka (KRaft 모드, 8개 토픽, 9개 Consumer) |
| AI | OpenAI Codex (OpenAI 게이트웨이) |
| Automation | n8n (Webhook → Discord/Slack/Email) |
| Crawling | Playwright 1.49 |
| Payment | Stripe + Toss Payments (Strategy 패턴) |
| Monitoring | Prometheus + Grafana |
| Infra | Docker Compose, Gradle 멀티 모듈 (5개 모듈) |

### 시스템 아키텍처

![AnchorIQ 시스템 아키텍처](/Users/sanghyunyoun/anchoriq/온톨로지.png)

## 2. 개발 방식 — 설계 주도 Vibe Coding

1인 풀스택으로 174개 API, 11개 데이터 수집기, 5개 대시보드 뷰를 구현해야 했습니다. 21개 설계 문서를 먼저 작성하고, Claude Code에 AGENTS.md(코딩 규칙 15개)와 CLAUDE.md(프로젝트 컨텍스트)를 통해 일관된 코드 생성을 강제했습니다.

### 설계는 직접, 구현은 AI에게

모든 기능을 구현 전에 마크다운 문서(.md)로 상세히 설계했습니다. API 엔드포인트 174개, 패키지 구조, DB 스키마, Kafka 토픽, 트랜잭션 전략까지 11개 Phase별 설계서(2,240줄)를 작성했고, AGENTS.md에 Clean Code + OOP + SOLID + DDD 규칙을 정의하여 AI가 항상 동일한 아키텍처와 컨벤션으로 코드를 생성하도록 제약을 걸었습니다.

"만들어줘"가 아니라 "이 설계서대로, 이 원칙대로 만들어라"라고 지시하는 방식이었고, 설계 문서가 충분히 상세했기 때문에 전체에 일관된 레이어 구조와 패턴이 유지되었습니다.

### AI 도구 + MCP 활용

| 도구 | 용도 | 효과 |
|------|------|------|
| Claude Code | 코드 생성 + 디버깅 + 리팩토링 | 설계서 기반 일관된 코드 생성 |
| Claude Teams | 설계 토론, 기술 선정 비교, 아키텍처 리뷰 | 21개 설계 문서 공동 작성 |
| Claude Skills | /commit, /review-pr 등 반복 작업 자동화 | 커밋 메시지 품질 + PR 리뷰 |
| Playwright MCP | 브라우저 자동 테스트 (로그인→페이지 순회→에러 수집) | 프론트 에러 11개 자동 발견 |
| Chrome DevTools MCP | 콘솔 에러, 네트워크 요청, 스크린샷 | API 응답 형식 불일치 진단 |
| Figma MCP | 디자인 스펙 추출, 컴포넌트 구조 참조 | 디자인→코드 변환 자동화 |

### 문서화

운영 중 발생한 트러블슈팅 기록 5건(docs/)을 작성하여, 문제→원인→해결→CS원리를 정리했습니다.

## 3. 기술 구조와 설계 판단

### 3-1. 모듈러 모놀리스 + 헥사고날 아키텍처

#### 배경 및 문제정의
- **상황**: 5개 도메인(수집/도메인/API/AI/자동화)을 하나의 프로젝트에서 관리
- **문제**: MSA는 1인 개발에 오버엔지니어링, 모놀리스는 모듈 경계 없이 스파게티

#### 기술 선정: MSA vs 모듈러 모놀리스

| 항목 | MSA | 모듈러 모놀리스 |
|------|------|----------|
| 인프라 비용 | 3.75~6배 | 1배 |
| 개발 복잡도 | 서비스간 통신, 분산 트랜잭션 | 모듈 경계만 관리 |
| 확장성 | 서비스 단위 스케일링 | 추후 모듈 단위 분리 가능 |
| 실무 트렌드 | 42% 기업이 모놀리스 회귀 (2025 CNCF) | Amazon Prime Video 90% 비용 절감 |

#### 솔루션: 5개 Gradle 모듈 + DIP

```
anchoriq-core/        → 순수 도메인 (외부 의존 금지)
anchoriq-api/         → REST + Security + Infrastructure
anchoriq-collector/   → 데이터 수집 + Kafka Producer
anchoriq-ai/          → OpenAI Codex + 리스크 판단
anchoriq-automation/  → n8n + Kafka Consumer + 알림
```

core 모듈은 Spring 어노테이션 외에 인프라 의존성이 없습니다. Repository는 인터페이스만 정의하고, 구현체는 api 모듈의 infrastructure 레이어에 위치합니다 (DIP).

### 3-2. Neo4j 온톨로지 + 4홉 Cypher 쿼리

#### 배경 및 문제정의
- **상황**: "이란 연관 회사가 소유한 선박 중 호르무즈 해협 접근 중인 탱커"를 찾아야 함
- **문제**: RDBMS JOIN으로는 4단계 관계 탐색이 비효율적

#### 솔루션: Neo4j 그래프 + 인덱스

```cypher
MATCH (v:Vessel)-[:OWNED_BY]->(c:Company)-[:REGISTERED_IN]->(co:Country)-[:SANCTIONED_BY]->(s:Sanction)
WHERE v.type = 'TANKER'
WITH v
MATCH (v)-[:SAILING_ON]->(r:Route)-[:PASSES_THROUGH]->(cp:Chokepoint {name: 'Hormuz'})
RETURN v.name, v.imo, v.flag
```

인덱스: IMO, MMSI, LOCODE, ISO Code에 유니크 제약 + 인덱스 생성.

### 3-3. Kafka 이벤트 스트림 + 3-Tier 트랜잭션

#### 배경 및 문제정의
- **상황**: AIS 선박 위치 데이터 초당 수백 건 + 리스크 알림 + 뉴스
- **문제**: 동기 처리하면 병목, 하나의 이벤트를 여러 소비자가 독립 처리해야 함

#### 솔루션: 8개 토픽 + 9개 Consumer Group

| 토픽 | 파티션 | Consumer Group | 역할 |
|------|--------|---------------|------|
| ais-positions | 3 | ais-neo4j-updater, ais-redis-writer | Neo4j + Redis GEO |
| risk-alerts | 2 | alert-n8n-trigger, alert-ws-pusher | n8n + WebSocket |
| sanction-updates | 1 | sanction-neo4j-updater, sanction-redis-cache | Neo4j + Redis |

3-Tier 트랜잭션:
- **Tier 1 (강한 일관성)**: 돈/유저 → `@Transactional` (PostgreSQL)
- **Tier 2 (최종 일관성)**: 온톨로지 → Kafka 이벤트 기반 (Neo4j)
- **Tier 3 (불필요)**: 캐시/로그 → 실패 무시 (Redis/ES)

### 3-4. n8n + Discord 자동 알림 파이프라인

#### 배경 및 문제정의
- **상황**: 리스크 이벤트 발생 시 팀에 즉시 알림 필요
- **문제**: 알림 채널(Slack/Discord/Email)이 다양, 규칙 기반 필터링 필요

#### 솔루션: Kafka Consumer → NotificationDispatcher → n8n Webhook

```
Kafka "risk-alerts"
  → RiskAlertN8nConsumer
    → N8nClient.triggerWebhook()      → n8n → Discord
    → NotificationDispatcher.dispatch() → SlackNotifier/EmailNotifier
    → WorkflowExecution 기록
  → RiskAlertWebSocketConsumer
    → /topic/alerts 실시간 푸시
```

`NotificationRule` 엔티티가 이벤트 타입/리스크 레벨 매칭 → 채널별 Notifier 인터페이스 → Strategy 패턴으로 발송.

### 3-5. 풍부한 도메인 모델 (Rich Domain Model)

#### 배경 및 문제정의
- **상황**: 리스크 판단 로직이 선박의 국적, 선령, 타입, 제재 여부에 강하게 의존
- **문제**: Service에 로직이 다 있으면 빈약한 도메인 (Anemic Domain Model)

#### 솔루션: Entity에 비즈니스 로직, Service는 오케스트레이션만

```java
// Vessel.java — Entity가 직접 리스크 평가
public int evaluateRiskScore(Set<String> sanctionedCodes, Set<String> highRiskFlags) {
    int score = 0;
    if (isRegisteredInSanctionedCountry(sanctionedCodes)) score += 40;
    if (highRiskFlags.contains(this.flag.value())) score += 20;
    if (calculateAge() >= 20) score += 15;
    if (isTanker()) score += 10;
    return Math.min(score, 100);
}

// RiskLevel.java — 도메인 Value Object에 분류 로직
public static RiskLevel fromScore(int score) {
    if (score >= 75) return CRITICAL;
    if (score >= 50) return HIGH;
    if (score >= 25) return MEDIUM;
    return LOW;
}
```

Value Object: `Imo`, `Mmsi`, `Flag`, `Locode`, `CongestionLevel`, `RiskLevel` — 원시 타입 포장으로 타입 안전성 확보.

### 3-6. @RequiredArgsConstructor 일관성 통일

#### 배경 및 문제정의
- **상황**: 48개 Spring Bean 클래스에서 수동 `this.xxx = xxx` 생성자와 Lombok 혼재
- **문제**: 일관성 위반, 불필요한 보일러플레이트 350줄

#### 솔루션: 전체 통일 + 예외 케이스 분리

JwtTokenProvider(SecretKey 변환 필요) → `JwtConfig`에서 `@Bean`으로 SecretKey 분리
BusinessMetrics(Counter/Timer 초기화) → `@PostConstruct`로 초기화 분리

결과: 48개 파일 `@RequiredArgsConstructor` 100% 통일, 예외 0개.

## 4. 주요 문제 해결 사례

### 4-1. Neo4j TransactionManager null → 멀티 데이터소스 설정

#### 배경 및 문제정의
- **상황**: 앱 시작 시 GeographyInitializer가 Neo4j에 데이터 시딩
- **문제**: 모든 Neo4j Repository 호출에서 `TransactionTemplate null` 에러, 데이터 0건

#### 분석
JPA + Neo4j 2개 데이터소스 공존 시 Spring Boot가 JPA `TransactionManager`를 기본으로 사용하고, Neo4j용은 자동 생성하지 못함.

#### 솔루션

```java
@Configuration
public class Neo4jConfig {
    @Primary
    @Bean("transactionManager")
    public PlatformTransactionManager jpaTransactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    @Bean("neo4jTransactionManager")
    public PlatformTransactionManager neo4jTransactionManager(Driver driver) {
        return new Neo4jTransactionManager(driver);
    }
}
```

#### 결과

| 지표 | Before | After |
|------|--------|-------|
| 초크포인트 시딩 | 0개 (에러) | 6개 |
| 항만 시딩 | 0개 (에러) | 20개 |
| 선박 시딩 | 0개 (에러) | 25개 |

### 4-2. Kafka Consumer Bean 미등록 → @ConditionalOnProperty 전환

#### 배경 및 문제정의
- **상황**: Kafka Docker 실행 중인데 Spring Boot에서 Consumer 0개 등록
- **문제**: `@ConditionalOnBean(KafkaTemplate.class)` — Bean 생성 순서 의존

#### 분석
Spring의 `@Configuration`이 `@AutoConfiguration`보다 먼저 처리되므로, KafkaTemplate Bean이 아직 없는 시점에 조건 평가.

#### 솔루션

```java
// Before — Bean 순서 의존 (불안정)
@ConditionalOnBean(KafkaTemplate.class)

// After — 프로퍼티 존재로 판단 (안정)
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
```

#### 결과

| 지표 | Before | After |
|------|--------|-------|
| Kafka Consumer | 0개 | 9개 |
| risk-alerts 파이프라인 | 미동작 | Kafka → n8n → Discord 자동 |

### 4-3. 프론트엔드 CRITICAL 크래시 2건 → API 응답 형식 통일

#### 배경 및 문제정의
- **상황**: Workflows, Alerts 페이지에서 ErrorBoundary 발동 (페이지 사용 불가)
- **문제**: `TypeError: workflows.map is not a function`

#### 분석
백엔드가 페이지네이션 객체 `{content: [], totalPages: N}`를 반환하는데, 프론트에서 `data.data`를 배열로 가정. `data.data ?? []`는 객체일 때 폴백 안 됨.

#### 솔루션

```typescript
// 배열이면 그대로, 페이지네이션 객체면 content 추출
setWorkflows(Array.isArray(data.data) ? data.data : (data.data?.content ?? []))
```

백엔드도 `PageResponse.of()` 팩토리 메서드 추가하여 모든 리스트 API를 통일된 페이지네이션 형식으로 반환.

### 4-4. n8n Webhook Expression 미동작 → JSON.stringify 전환

#### 배경 및 문제정의
- **상황**: n8n Webhook → Discord HTTP Request에서 `{{ $json.body.riskLevel }}`이 undefined
- **문제**: 중간 Edit Fields 노드가 데이터를 덮어쓰고, 표현식 문법 불일치

#### 솔루션
Edit Fields 노드 제거 + `JSON.stringify()` JavaScript 표현식으로 전환.

#### 결과
```
Discord 알림:
🚨 [CRITICAL] SANCTION_VESSEL_DETECTED
- 선박: SHAHR-E-KORD (IMO: 9167253)
- 사유: 이란 연관 회사 소유 선박 호르무즈 해협 접근
- 권장: 컴플라이언스팀 즉시 확인
```

## 5. 설계/기술 문서 목록 (docs/)

### 설계 판단 기록 (10건)

| 문서 | 핵심 내용 |
|------|---------|
| `DESIGN_NEO4J_ONTOLOGY_VS_RDBMS.md` | Neo4j vs RDBMS 비교 — 4홉 관계 탐색에서 그래프 DB 선택 근거 |
| `DESIGN_DDD_RICH_DOMAIN_MODEL.md` | 풍부한 도메인 모델 — Entity에 비즈니스 로직, Service는 오케스트레이션만 |
| `DESIGN_KAFKA_EVENT_PIPELINE.md` | Kafka 8개 토픽 설계 — 파티션 키, Consumer Group, DLT 전략 |
| `DESIGN_MULTI_DB_ARCHITECTURE.md` | 4개 DB 선정 — Neo4j/PostgreSQL/Redis/ES 역할 분리 근거 |
| `DESIGN_PAYMENT_STRATEGY_PATTERN.md` | Stripe + Toss Strategy 패턴 — 통화 기반 게이트웨이 분기 |
| `DESIGN_REALTIME_WEBSOCKET_ARCHITECTURE.md` | WebSocket STOMP 3채널 — 선박/알림/대시보드 실시간 |
| `DESIGN_VALUE_OBJECT_PATTERN.md` | Value Object 7개 — Imo/Mmsi/Flag/Locode 등 원시 타입 포장 |
| `DESIGN_AGGREGATE_ROOT_EVENT.md` | Aggregate Root + Domain Event — 상태 변경 시 이벤트 발행 |
| `DESIGN_INTERFACE_BASED_SERVICE.md` | 인터페이스 기반 설계 — 모든 Service/Repository/Gateway 인터페이스 우선 |
| `DESIGN_CONFIGURATION_BEAN_PATTERN.md` | @Bean 명시적 등록 — @Component 대신 Config에서 순수 POJO 관리 |

### 트러블슈팅 기록 (5건)

| 문서 | Before → After |
|------|---------------|
| `TROUBLESHOOTING_NEO4J_TRANSACTION_MANAGER_NULL.md` | 데이터 시딩 0건 → 62건 (Neo4j TM 멀티 데이터소스 충돌) |
| `TROUBLESHOOTING_KAFKA_CONDITIONAL_BEAN_REGISTRATION.md` | Consumer 0개 → 9개 (Bean 생성 순서 → @ConditionalOnProperty) |
| `REFACTORING_MANUAL_CONSTRUCTOR_TO_LOMBOK.md` | 수동 생성자 48개 → @RequiredArgsConstructor 100% 통일 |
| `N8N_KAFKA_DISCORD_PIPELINE.md` | 알림 미동작 → Kafka → n8n → Discord 전체 파이프라인 구축 |
| `FRONTEND_ERROR_REPORT.md` | CRITICAL 크래시 2건 + HIGH 8건 → 전부 해결 |

### 기타

| 문서 | 내용 |
|------|------|
| `AI_DECISION_ENGINE_DESIGN.md` | AI 의사결정 엔진 설계 — OpenAI Codex 연동 구조 |
| `PHASE6_AUTOMATION_ARCHITECTURE.md` | n8n 자동화 아키텍처 — 워크플로우/알림/타임라인 |
| `DESIGN_PORT_CONGESTION_DUAL_LAYER.md` | 항만 혼잡도 이중 레이어 — UNCTAD 기준선 + 실시간 AIS |
| `DESIGN_COLLECTOR_CONTROL_API.md` | 수집기 제어 API — 동적 시작/중지/스케줄 관리 |
| `LOAD_TEST_RESULTS.md` | k6 부하 테스트 — 200명 동시접속 p95 응답 5.79ms |

## 6. 회고

이 프로젝트에서 가장 많이 배운 건 **설계 문서의 힘**이었습니다. 21개 설계 문서를 먼저 쓰고 AI에게 구현을 맡겼더니, 174개 API 전체에 일관된 아키텍처가 유지되었습니다. "만들어줘"가 아니라 "이 설계서대로 만들어라"의 차이는 컸습니다.

멀티 데이터소스(Neo4j + PostgreSQL + Redis + ES) 환경에서 트랜잭션 매니저 충돌, Kafka Bean 생성 순서 문제, n8n 표현식 디버깅 같은 문제들은 AI가 해결해주지 않았습니다. 로그를 직접 읽고, 스택 트레이스를 추적하고, Spring 내부 동작 원리를 이해해야 풀 수 있는 문제였습니다.

Claude Code + MCP(Playwright, Chrome DevTools)를 활용한 자동 테스트 — 브라우저를 띄워서 모든 페이지를 순회하며 콘솔 에러를 수집하고, API 응답을 검증하는 워크플로 — 는 수동 QA 시간을 크게 줄여줬습니다. **AI는 80%를 가속하지만, 나머지 20%의 아키텍처 판단과 디버깅은 여전히 개발자의 몫**이라는 것이 이 프로젝트의 가장 실무적인 교훈입니다.
