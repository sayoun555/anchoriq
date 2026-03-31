# AnchorIQ — 패키지 구조

> 5개 모듈 × DDD 레이어 — 인터페이스 기반 + 도메인 그룹핑 + 기능별 파일 분리
> 원칙: 200줄 넘을 것 같으면 분리, 인터페이스 먼저 정의, 사람과 AI 모두 추적 쉽게

---

## 목차
- [설계 원칙](#설계-원칙)
- [전체 구조](#전체-구조)
- [anchoriq-core](#anchoriq-core)
- [anchoriq-api](#anchoriq-api)
- [anchoriq-collector](#anchoriq-collector)
- [anchoriq-ai](#anchoriq-ai)
- [anchoriq-automation](#anchoriq-automation)
- [Frontend](#frontend)

---

## 설계 원칙

### 인터페이스 기반

```
모든 Service, Repository, Gateway → 인터페이스 먼저 정의
구현체는 별도 파일로 분리

왜?
- AI가 인터페이스만 보면 "뭘 하는 건지" 바로 파악
- 구현체 바꿔도 호출하는 쪽 안 건드림
- 테스트할 때 Mock 주입 쉬움
```

### 도메인 그룹핑

```
18개 도메인을 4개 그룹으로:
  maritime/       ← 해운 (핵심)
  intelligence/   ← 리스크/AI 판단
  account/        ← 유저/결제
  operation/      ← 자동화/관리
```

### 기능별 파일 분리

```
파일 하나가 200줄 넘을 것 같으면 → 분리
메서드가 7개 넘으면 → 분리
서로 다른 책임이면 → 분리
```

---

## 전체 구조

```
/backend/
  /anchoriq-core/src/main/java/com/anchoriq/core/
  /anchoriq-api/src/main/java/com/anchoriq/api/
  /anchoriq-collector/src/main/java/com/anchoriq/collector/
  /anchoriq-ai/src/main/java/com/anchoriq/ai/
  /anchoriq-automation/src/main/java/com/anchoriq/automation/
```

---

## anchoriq-core

> 순수 도메인 — 비즈니스 로직의 심장
> Spring Web, Kafka, 외부 의존성 금지
> 모든 Repository, Service → 인터페이스로 정의

```
com.anchoriq.core/
│
├── domain/
│   │
│   ├── maritime/                             ← 해운 핵심 도메인
│   │   │
│   │   ├── vessel/
│   │   │   ├── model/
│   │   │   │   ├── Vessel.java               ← @Node (Neo4j), 리스크 판단 로직 보유
│   │   │   │   ├── VesselType.java           ← enum (TANKER, CONTAINER, BULK...)
│   │   │   │   ├── VesselStatus.java         ← enum (SAILING, ANCHORED, MOORED)
│   │   │   │   └── Imo.java                  ← VO (원시 타입 포장)
│   │   │   └── repository/
│   │   │       └── VesselRepository.java     ← 인터페이스
│   │   │
│   │   ├── port/
│   │   │   ├── model/
│   │   │   │   ├── Port.java                 ← @Node
│   │   │   │   ├── CongestionLevel.java      ← VO (0~100)
│   │   │   │   └── Locode.java               ← VO
│   │   │   └── repository/
│   │   │       └── PortRepository.java       ← 인터페이스
│   │   │
│   │   ├── route/
│   │   │   ├── model/
│   │   │   │   ├── Route.java                ← @Node
│   │   │   │   └── Chokepoint.java           ← @Node
│   │   │   └── repository/
│   │   │       ├── RouteRepository.java      ← 인터페이스
│   │   │       └── ChokepointRepository.java ← 인터페이스
│   │   │
│   │   ├── company/
│   │   │   ├── model/
│   │   │   │   └── Company.java              ← @Node
│   │   │   └── repository/
│   │   │       └── CompanyRepository.java    ← 인터페이스
│   │   │
│   │   ├── country/
│   │   │   ├── model/
│   │   │   │   └── Country.java              ← @Node
│   │   │   └── repository/
│   │   │       └── CountryRepository.java    ← 인터페이스
│   │   │
│   │   ├── sanction/
│   │   │   ├── model/
│   │   │   │   └── Sanction.java             ← @Node
│   │   │   └── repository/
│   │   │       └── SanctionRepository.java   ← 인터페이스
│   │   │
│   │   ├── weather/
│   │   │   ├── model/
│   │   │   │   ├── WeatherCondition.java     ← @Node
│   │   │   │   ├── WeatherType.java          ← enum (TYPHOON, STORM, FOG, CLEAR)
│   │   │   │   └── SeaZone.java              ← @Node
│   │   │   └── repository/
│   │   │       ├── WeatherRepository.java    ← 인터페이스
│   │   │       └── SeaZoneRepository.java    ← 인터페이스
│   │   │
│   │   └── eez/
│   │       ├── model/
│   │       │   └── Eez.java                  ← @Node
│   │       └── repository/
│   │           └── EezRepository.java        ← 인터페이스
│   │
│   ├── intelligence/                          ← 리스크/AI 판단 도메인
│   │   │
│   │   ├── risk/
│   │   │   ├── model/
│   │   │   │   ├── RiskAssessment.java       ← 판단 결과 (불변)
│   │   │   │   ├── RiskScore.java            ← VO (0~100)
│   │   │   │   ├── RiskLevel.java            ← enum (LOW, MEDIUM, HIGH, CRITICAL)
│   │   │   │   └── RiskType.java             ← enum (SANCTION, WEATHER, GEOPOLITICAL...)
│   │   │   ├── repository/
│   │   │   │   └── RiskAssessmentRepository.java  ← 인터페이스
│   │   │   └── service/
│   │   │       ├── SupplyChainRiskService.java          ← 인터페이스
│   │   │       ├── SupplyChainRiskServiceImpl.java      ← Vessel+Route+Weather+Sanction 조합
│   │   │       ├── SanctionScreeningService.java        ← 인터페이스
│   │   │       ├── SanctionScreeningServiceImpl.java    ← 제재 대조
│   │   │       ├── RouteOptimizationService.java        ← 인터페이스
│   │   │       ├── RouteOptimizationServiceImpl.java    ← 대체 항로 추천
│   │   │       ├── RouteComparisonService.java          ← 인터페이스
│   │   │       └── RouteComparisonServiceImpl.java      ← 항로/항만 비교
│   │   │
│   │   └── anomaly/
│   │       ├── model/
│   │       │   ├── AnomalyDetection.java     ← 이상 탐지 기록
│   │       │   └── AnomalyType.java          ← enum (AIS_OFF, ROUTE_DEVIATION, SPEED_CHANGE, DARK_SHIP)
│   │       ├── repository/
│   │       │   └── AnomalyRepository.java    ← 인터페이스
│   │       └── service/
│   │           ├── AnomalyDetectionService.java      ← 인터페이스
│   │           └── AnomalyDetectionServiceImpl.java   ← AIS 소실/항로 이탈/속도 이상 탐지
│   │
│   ├── account/                               ← 유저/결제 도메인
│   │   │
│   │   ├── user/
│   │   │   ├── model/
│   │   │   │   ├── User.java                 ← @Entity (JPA)
│   │   │   │   ├── Role.java                 ← enum (USER, ADMIN)
│   │   │   │   └── Email.java                ← VO
│   │   │   └── repository/
│   │   │       └── UserRepository.java       ← 인터페이스
│   │   │
│   │   ├── subscription/
│   │   │   ├── model/
│   │   │   │   ├── Subscription.java         ← @Entity (JPA), @Version (낙관적 락)
│   │   │   │   ├── Plan.java                 ← enum (FREE, PRO, ENTERPRISE)
│   │   │   │   ├── SubscriptionStatus.java   ← enum (ACTIVE, CANCELLED, EXPIRED)
│   │   │   │   └── Feature.java              ← enum (AI_QUERY, REALTIME_ALERT, WHATIF...)
│   │   │   ├── repository/
│   │   │   │   └── SubscriptionRepository.java  ← 인터페이스
│   │   │   └── service/
│   │   │       ├── SubscriptionService.java          ← 인터페이스
│   │   │       └── SubscriptionServiceImpl.java      ← 플랜별 기능 제한 판단
│   │   │
│   │   ├── payment/
│   │   │   ├── model/
│   │   │   │   ├── Payment.java              ← @Entity (JPA)
│   │   │   │   ├── PaymentStatus.java        ← enum (SUCCESS, FAILED, REFUNDED)
│   │   │   │   ├── PaymentGatewayType.java   ← enum (STRIPE, TOSS)
│   │   │   │   └── Currency.java             ← enum (USD, KRW)
│   │   │   ├── repository/
│   │   │   │   └── PaymentRepository.java    ← 인터페이스
│   │   │   └── gateway/
│   │   │       └── PaymentGateway.java       ← 인터페이스 (Strategy 패턴)
│   │   │
│   │   └── apikey/
│   │       ├── model/
│   │       │   └── ApiKey.java               ← @Entity (JPA)
│   │       └── repository/
│   │           └── ApiKeyRepository.java     ← 인터페이스
│   │
│   └── operation/                             ← 운영/자동화 도메인
│       │
│       ├── workflow/
│       │   ├── model/
│       │   │   ├── Workflow.java             ← @Entity (JPA), @Version
│       │   │   ├── WorkflowStatus.java       ← enum (ACTIVE, INACTIVE)
│       │   │   └── WorkflowExecution.java    ← @Entity (JPA)
│       │   └── repository/
│       │       ├── WorkflowRepository.java           ← 인터페이스
│       │       └── WorkflowExecutionRepository.java  ← 인터페이스
│       │
│       ├── notification/
│       │   ├── model/
│       │   │   ├── NotificationRule.java     ← @Entity (JPA), @Version
│       │   │   └── NotificationChannel.java  ← enum (SLACK, EMAIL)
│       │   └── repository/
│       │       └── NotificationRuleRepository.java  ← 인터페이스
│       │
│       ├── bookmark/
│       │   ├── model/
│       │   │   ├── Watchlist.java            ← @Entity (JPA)
│       │   │   ├── Bookmark.java             ← @Entity (JPA)
│       │   │   └── BookmarkTargetType.java   ← enum (PORT, ROUTE, CHOKEPOINT)
│       │   └── repository/
│       │       ├── WatchlistRepository.java  ← 인터페이스
│       │       └── BookmarkRepository.java   ← 인터페이스
│       │
│       └── audit/
│           ├── model/
│           │   ├── AuditLog.java             ← @Entity (JPA)
│           │   └── AuditAction.java          ← enum (LOGIN, QUERY, EXPORT...)
│           └── repository/
│               └── AuditLogRepository.java   ← 인터페이스
│
├── event/                                     ← 도메인 이벤트
│   ├── RiskAlertEvent.java
│   ├── AnomalyDetectedEvent.java
│   ├── SanctionUpdatedEvent.java
│   └── WeatherAlertEvent.java
│
└── common/
    ├── exception/
    │   ├── DomainException.java              ← 도메인 예외 베이스
    │   ├── EntityNotFoundException.java       ← 범용 엔티티 미발견
    │   ├── PaymentFailedException.java
    │   ├── PlanLimitExceededException.java
    │   └── ConcurrentRequestException.java
    └── vo/
        ├── Coordinate.java                   ← VO (lat, lon)
        └── DateRange.java                    ← VO (from, to)
```

---

## anchoriq-api

> REST Controller + DTO + Security + 인프라 구현체
> Controller는 기능별 분리 (200줄 초과 방지)

```
com.anchoriq.api/
│
├── AnchoriqApplication.java                  ← @SpringBootApplication (메인)
│
├── controller/
│   │
│   ├── auth/
│   │   └── AuthController.java               ← 회원가입, 로그인, 토큰 갱신, 로그아웃
│   │
│   ├── vessel/
│   │   ├── VesselQueryController.java        ← 목록, 상세, 주변 선박, 통계
│   │   ├── VesselRiskController.java         ← 리스크 조회, 제재 선박
│   │   ├── VesselRelationController.java     ← 온톨로지 관계, 이력, 항로, 이벤트
│   │   └── VesselWatchlistController.java    ← 관심 선박 등록/해제/목록
│   │
│   ├── port/
│   │   ├── PortQueryController.java          ← 목록, 상세, 통계, 랭킹
│   │   └── PortDetailController.java         ← 혼잡도, 정박 선박, 이력, ETA
│   │
│   ├── route/
│   │   └── RouteController.java              ← 항로 목록, 초크포인트 (7개 API, 분리 불필요)
│   │
│   ├── risk/
│   │   ├── RiskDashboardController.java      ← 워룸 대시보드, 히트맵
│   │   ├── RiskAlertController.java          ← 알림 목록, 상세, 확인, 무시
│   │   ├── RiskScoreController.java          ← 선박/항로/초크포인트/항만 스코어
│   │   └── RiskReportController.java         ← 추세, 일일/주간 리포트, 타임라인
│   │
│   ├── ai/
│   │   ├── AiQueryController.java            ← 자연어 질의, 사용량 조회
│   │   ├── AiBriefingController.java         ← 일일 브리핑, AI 판단 이력
│   │   ├── AiWhatIfController.java           ← What-if 시뮬레이션, 이력, 템플릿
│   │   └── AiReportController.java           ← AI 리포트 생성/조회, 추천 액션
│   │
│   ├── ontology/
│   │   └── OntologyController.java           ← 그래프, 노드 펼침, 검색, 경로, 통계
│   │
│   ├── sanction/
│   │   ├── SanctionQueryController.java      ← 목록, 상세, 국가
│   │   └── SanctionScreeningController.java  ← 선박/회사 스크리닝, 변경 이력
│   │
│   ├── weather/
│   │   └── WeatherController.java            ← 기상, 태풍, 예보, 영향 선박
│   │
│   ├── anomaly/
│   │   └── AnomalyController.java            ← AIS 소실, 항로 이탈, 속도 이상, 다크쉽
│   │
│   ├── market/
│   │   ├── OilPriceController.java           ← 유가 현재/이력/영향
│   │   ├── ExchangeRateController.java       ← 환율 현재/이력/계산
│   │   └── FreightController.java            ← 운임 지수, 항로별 비용
│   │
│   ├── news/
│   │   └── NewsController.java               ← 뉴스, 지정학 이벤트
│   │
│   ├── map/
│   │   └── MapController.java                ← 지도 데이터 (선박, 초크포인트, EEZ, 히트맵)
│   │
│   ├── dashboard/
│   │   └── DashboardController.java          ← 위젯 (요약, 초크포인트, TOP 리스크, 추세)
│   │
│   ├── workflow/
│   │   └── WorkflowController.java           ← CRUD, 활성화/비활성화, 실행 이력
│   │
│   ├── notification/
│   │   ├── NotificationSettingController.java ← 설정 조회/변경, 테스트 발송, 이력
│   │   └── NotificationRuleController.java    ← 규칙 CRUD
│   │
│   ├── payment/
│   │   ├── PaymentController.java            ← 결제 이력
│   │   ├── SubscriptionController.java       ← 구독 플랜, 결제, 취소
│   │   └── WebhookController.java            ← Stripe/Toss 웹훅 수신
│   │
│   ├── user/
│   │   └── UserController.java               ← 프로필, 비밀번호, 탈퇴, 활동
│   │
│   ├── bookmark/
│   │   └── BookmarkController.java           ← 즐겨찾기 CRUD
│   │
│   ├── search/
│   │   └── SearchController.java             ← 글로벌 검색, 자동완성
│   │
│   ├── compare/
│   │   └── CompareController.java            ← 항로/항만 비교
│   │
│   ├── export/
│   │   └── ExportController.java             ← PDF/CSV 내보내기
│   │
│   ├── external/
│   │   └── ExternalApiController.java        ← Enterprise 외부 API + API 키
│   │
│   ├── admin/
│   │   ├── AdminUserController.java          ← 유저 목록, 상세, 권한/구독 변경
│   │   ├── AdminSystemController.java        ← 시스템 통계, API 사용량, 캐시
│   │   └── AdminPipelineController.java      ← 파이프라인 상태, 수동 트리거, Kafka lag
│   │
│   └── system/
│       └── SystemController.java             ← 데이터 상태, 감사 로그
│
├── dto/
│   ├── request/
│   │   ├── auth/
│   │   │   ├── SignupRequest.java
│   │   │   └── LoginRequest.java
│   │   ├── payment/
│   │   │   └── SubscribeRequest.java
│   │   ├── ai/
│   │   │   ├── AiQueryRequest.java
│   │   │   └── WhatIfRequest.java
│   │   ├── workflow/
│   │   │   └── WorkflowCreateRequest.java
│   │   ├── notification/
│   │   │   └── NotificationRuleRequest.java
│   │   └── ...
│   └── response/
│       ├── vessel/
│       │   ├── VesselResponse.java
│       │   └── VesselRiskResponse.java
│       ├── port/
│       │   └── PortResponse.java
│       ├── risk/
│       │   ├── RiskAlertResponse.java
│       │   └── RiskScoreResponse.java
│       ├── ai/
│       │   ├── AiQueryResponse.java
│       │   └── AiBriefingResponse.java
│       ├── dashboard/
│       │   └── DashboardSummaryResponse.java
│       └── common/
│           ├── PageResponse.java             ← 공통 페이지네이션
│           └── ErrorResponse.java            ← 공통 에러
│
├── application/
│   ├── auth/
│   │   └── AuthApplicationService.java
│   ├── vessel/
│   │   ├── VesselQueryApplicationService.java
│   │   └── VesselWatchlistApplicationService.java
│   ├── payment/
│   │   ├── PaymentApplicationService.java    ← 결제 오케스트레이션 (보상 트랜잭션)
│   │   └── SubscriptionApplicationService.java
│   ├── risk/
│   │   ├── RiskDashboardApplicationService.java
│   │   ├── RiskAlertApplicationService.java
│   │   ├── RiskScoreApplicationService.java
│   │   └── RiskReportApplicationService.java
│   ├── workflow/
│   │   └── WorkflowApplicationService.java
│   ├── export/
│   │   └── ExportApplicationService.java
│   └── search/
│       └── SearchApplicationService.java
│
├── infrastructure/
│   ├── security/
│   │   ├── SecurityConfig.java               ← Spring Security 설정
│   │   ├── JwtTokenProvider.java             ← JWT 생성/검증
│   │   ├── JwtAuthenticationFilter.java      ← 요청마다 JWT 검증
│   │   └── PlanAuthorizationFilter.java      ← 플랜별 접근 제한
│   ├── payment/
│   │   ├── StripeGateway.java                ← PaymentGateway 구현체
│   │   ├── TossGateway.java                  ← PaymentGateway 구현체
│   │   └── PaymentGatewayRouter.java         ← 통화 기반 분기
│   ├── websocket/
│   │   ├── WebSocketConfig.java              ← STOMP 설정
│   │   └── WebSocketEventHandler.java
│   ├── export/
│   │   ├── PdfReportBuilder.java             ← OpenPDF
│   │   └── CsvExporter.java                  ← OpenCSV
│   └── config/
│       ├── CorsConfig.java                   ← CORS 글로벌 설정
│       ├── SwaggerConfig.java
│       └── RedisConfig.java
│
├── annotation/
│   └── RequiresPlan.java                     ← @RequiresPlan(Plan.PRO)
│
└── global/
    ├── error/
    │   └── GlobalExceptionHandler.java       ← @RestControllerAdvice
    └── response/
        └── ApiResponse.java                  ← 공통 응답 래퍼
```

---

## anchoriq-collector

> 11개 데이터 수집기 + Kafka Producer/Consumer
> 수집기마다 인터페이스 → 구현체 분리

```
com.anchoriq.collector/
│
├── source/                                    ← 데이터 수집 (Producer)
│   │
│   ├── DataCollector.java                    ← 인터페이스 (공통)
│   │
│   ├── ais/
│   │   ├── AisStreamClient.java              ← 인터페이스
│   │   ├── AisStreamWebSocketClient.java     ← WebSocket 구현체
│   │   └── AisMessageParser.java
│   │
│   ├── weather/
│   │   ├── WeatherCollector.java             ← 인터페이스
│   │   └── OpenMeteoCollector.java           ← 구현체
│   │
│   ├── sanction/
│   │   ├── SanctionCollector.java            ← 인터페이스
│   │   ├── UnitedNationsSanctionCollector.java
│   │   └── OpenSanctionsCollector.java
│   │
│   ├── news/
│   │   ├── NewsCollector.java                ← 인터페이스
│   │   └── GNewsCollector.java               ← 구현체
│   │
│   ├── market/
│   │   ├── OilPriceCollector.java            ← 인터페이스
│   │   ├── EiaOilPriceCollector.java         ← 구현체
│   │   ├── ExchangeRateCollector.java        ← 인터페이스
│   │   └── FrankfurterCollector.java         ← 구현체
│   │
│   ├── geopolitical/
│   │   ├── GeopoliticalCollector.java        ← 인터페이스
│   │   └── GdeltCollector.java               ← 구현체
│   │
│   ├── port/
│   │   ├── PortDataCollector.java            ← 인터페이스
│   │   ├── UnLocodeLoader.java               ← 벌크 로드
│   │   └── UncladCongestionCrawler.java      ← Playwright 크롤링
│   │
│   └── geography/
│       ├── GeographyLoader.java              ← 인터페이스
│       ├── MarineRegionsLoader.java          ← EEZ GeoJSON
│       ├── NaturalEarthLoader.java           ← 초크포인트 좌표
│       └── GeographyInitializer.java         ← 앱 시작 시 정적 데이터 로드
│
├── producer/                                  ← Kafka Producer (토픽별)
│   ├── KafkaMessageProducer.java             ← 인터페이스 (공통)
│   ├── AisKafkaProducer.java
│   ├── WeatherKafkaProducer.java
│   ├── SanctionKafkaProducer.java
│   ├── NewsKafkaProducer.java
│   ├── MarketKafkaProducer.java
│   ├── GeopoliticalKafkaProducer.java
│   └── PortCongestionKafkaProducer.java
│
├── consumer/                                  ← Kafka Consumer (Consumer Group별)
│   ├── ais/
│   │   ├── AisRedisConsumer.java             ← ais-positions → Redis GEO
│   │   └── AisNeo4jConsumer.java             ← ais-positions → Neo4j 상태
│   ├── weather/
│   │   └── WeatherNeo4jConsumer.java         ← weather-events → Neo4j
│   ├── sanction/
│   │   ├── SanctionNeo4jConsumer.java        ← sanction-updates → Neo4j
│   │   └── SanctionRedisConsumer.java        ← sanction-updates → Redis 캐시
│   ├── news/
│   │   └── NewsElasticsearchConsumer.java    ← news-events → Elasticsearch
│   ├── market/
│   │   └── MarketDataConsumer.java           ← market-data → Redis + Neo4j
│   ├── geopolitical/
│   │   └── GeopoliticalConsumer.java         ← geopolitical-events → ES + Neo4j
│   └── port/
│       └── PortCongestionConsumer.java       ← port-congestion → Neo4j + Redis
│
├── scheduler/
│   └── CollectorScheduler.java               ← @Scheduled 수집 주기 관리
│
├── config/
│   ├── KafkaTopicConfig.java                 ← 토픽 생성 @Bean
│   ├── KafkaProducerConfig.java
│   ├── KafkaConsumerConfig.java
│   └── PlaywrightConfig.java                 ← Playwright 브라우저 설정
│
└── common/
    ├── CollectorException.java
    └── RetryHandler.java                     ← 수집 실패 재시도 로직
```

---

## anchoriq-ai

> OpenClaw 연동 + AI 의사결정 엔진
> 기능별 분리: 질의 / 스코어링 / 브리핑 / 시뮬레이션 / 리포트

```
com.anchoriq.ai/
│
├── client/
│   ├── AiClient.java                         ← 인터페이스
│   ├── OpenClawClient.java                   ← 구현체
│   ├── OpenClawRequest.java
│   └── OpenClawResponse.java
│
├── query/
│   ├── NaturalLanguageQueryService.java      ← 인터페이스
│   ├── NaturalLanguageQueryServiceImpl.java  ← 자연어 → Cypher 변환
│   └── CypherQueryBuilder.java
│
├── scoring/
│   ├── RiskScorer.java                       ← 인터페이스 (공통)
│   ├── VesselRiskScorer.java                 ← 선박별 스코어
│   ├── RouteRiskScorer.java                  ← 항로별 스코어
│   ├── ChokepointRiskScorer.java             ← 초크포인트별 스코어
│   └── PortRiskScorer.java                   ← 항만별 스코어
│
├── briefing/
│   ├── BriefingService.java                  ← 인터페이스
│   ├── DailyBriefingServiceImpl.java         ← 일일 리스크 브리핑
│   └── WeeklyReportServiceImpl.java          ← 주간 리포트
│
├── whatif/
│   ├── WhatIfService.java                    ← 인터페이스
│   ├── WhatIfServiceImpl.java                ← What-if 시뮬레이션 실행
│   ├── WhatIfTemplate.java
│   └── WhatIfResult.java
│
├── recommendation/
│   ├── RecommendationService.java            ← 인터페이스
│   └── RecommendationServiceImpl.java        ← AI 추천 액션
│
├── report/
│   ├── AiReportService.java                  ← 인터페이스
│   └── AiReportServiceImpl.java              ← AI 리포트 생성
│
├── consumer/
│   ├── AisAnomalyDetectorConsumer.java       ← ais-positions → AI 이상 탐지
│   ├── WeatherRiskConsumer.java              ← weather-events → AI 기상 리스크
│   ├── GeopoliticalRiskConsumer.java         ← geopolitical-events → 초크포인트 리스크
│   └── NewsAnalyzerConsumer.java             ← news-events → AI 뉴스 분석
│
└── config/
    └── OpenClawConfig.java                   ← API 키, Base URL
```

---

## anchoriq-automation

> n8n 연동 + 알림 발송 + 타임라인 관리

```
com.anchoriq.automation/
│
├── n8n/
│   ├── N8nClient.java                        ← 인터페이스
│   ├── N8nWebhookClient.java                 ← 웹훅 HTTP 호출 구현체
│   ├── N8nWorkflowManager.java               ← n8n API 워크플로우 관리 구현체
│   └── N8nConfig.java
│
├── consumer/
│   ├── RiskAlertN8nConsumer.java             ← risk-alerts → n8n 트리거
│   ├── RiskAlertElasticsearchConsumer.java   ← risk-alerts → ES 로그 저장
│   └── RiskAlertWebSocketConsumer.java       ← risk-alerts → 프론트 WS 푸시
│
├── notification/
│   ├── NotificationDispatcher.java           ← 인터페이스
│   ├── NotificationDispatcherImpl.java       ← 알림 규칙 매칭 → 채널 발송
│   ├── Notifier.java                         ← 인터페이스 (채널별)
│   ├── SlackNotifier.java                    ← Slack 발송 구현체
│   └── EmailNotifier.java                    ← Email 발송 구현체
│
└── timeline/
    ├── TimelineService.java                  ← 인터페이스
    └── TimelineServiceImpl.java              ← 이벤트→판단→액션 이력 관리
```

---

## Frontend

```
/frontend/src/
│
├── main.tsx
├── App.tsx
│
├── api/
│   ├── client.ts                             ← Axios 인스턴스 (JWT 인터셉터, 401 자동 갱신)
│   ├── auth.ts
│   ├── vessels.ts
│   ├── ports.ts
│   ├── risk.ts
│   ├── ai.ts
│   ├── ontology.ts
│   ├── market.ts
│   ├── weather.ts
│   ├── sanctions.ts
│   ├── workflows.ts
│   ├── notifications.ts
│   ├── payments.ts
│   └── admin.ts
│
├── components/
│   ├── map/
│   │   ├── MapView.tsx                       ← Mapbox 지도
│   │   ├── VesselMarker.tsx
│   │   ├── ChokepointMarker.tsx
│   │   ├── RiskHeatmap.tsx
│   │   └── EezLayer.tsx
│   ├── graph/
│   │   ├── GraphView.tsx                     ← Cytoscape.js
│   │   └── GraphNode.tsx
│   ├── timeline/
│   │   └── TimelineView.tsx
│   ├── dashboard/
│   │   ├── WarRoom.tsx
│   │   ├── RiskSummary.tsx
│   │   ├── ChokepointStatus.tsx
│   │   ├── AlertFeed.tsx
│   │   └── StatWidget.tsx
│   ├── ai/
│   │   ├── QueryBar.tsx
│   │   └── AiResponse.tsx
│   ├── chart/
│   │   ├── RiskTrendChart.tsx
│   │   ├── OilPriceChart.tsx
│   │   └── ExchangeRateChart.tsx
│   └── common/
│       ├── Layout.tsx
│       ├── Navbar.tsx
│       ├── Sidebar.tsx
│       ├── LoadingSpinner.tsx
│       └── ErrorBoundary.tsx
│
├── pages/
│   ├── LoginPage.tsx
│   ├── SignupPage.tsx
│   ├── DashboardPage.tsx
│   ├── MapPage.tsx
│   ├── GraphPage.tsx
│   ├── TimelinePage.tsx
│   ├── VesselDetailPage.tsx
│   ├── PortDetailPage.tsx
│   ├── AiPage.tsx
│   ├── WhatIfPage.tsx
│   ├── MarketPage.tsx
│   ├── WeatherPage.tsx
│   ├── SanctionsPage.tsx
│   ├── WorkflowsPage.tsx
│   ├── NotificationsPage.tsx
│   ├── SettingsPage.tsx
│   ├── ComparePage.tsx
│   ├── AdminPage.tsx
│   └── NotFoundPage.tsx
│
├── hooks/
│   ├── useAuth.ts
│   ├── useWebSocket.ts
│   ├── useVessels.ts
│   └── useRiskAlerts.ts
│
├── store/
│   ├── authStore.ts                          ← Zustand
│   ├── mapStore.ts
│   └── alertStore.ts
│
├── types/                                     ← 백엔드 DTO와 일치
│   ├── vessel.ts
│   ├── port.ts
│   ├── risk.ts
│   ├── ai.ts
│   └── ...
│
├── utils/
│   ├── formatDate.ts
│   ├── formatNumber.ts
│   └── riskColor.ts
│
└── styles/
    └── globals.css
```
