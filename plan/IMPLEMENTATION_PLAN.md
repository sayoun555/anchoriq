# AnchorIQ — 구현 계획

> 페이즈별 구현 순서 — 각 페이즈 완료 시 독립적으로 데모 가능

---

## 목차
- [구현 원칙](#구현-원칙)
- [Phase 1 — 기반 세팅](#phase-1--기반-세팅)
- [Phase 2 — 인증 + 결제](#phase-2--인증--결제)
- [Phase 3 — 데이터 수집](#phase-3--데이터-수집)
- [Phase 4 — 온톨로지 엔진](#phase-4--온톨로지-엔진)
- [Phase 5 — AI 의사결정](#phase-5--ai-의사결정)
- [Phase 6 — 자동화](#phase-6--자동화)
- [Phase 7 — 프론트엔드](#phase-7--프론트엔드)

---

## 구현 원칙

1. **각 페이즈가 끝나면 독립적으로 데모 가능**해야 함
2. **의존성 순서대로** 구현 (기반 → 위에 쌓기)
3. **AGENTS.md 규칙** 전체 적용 (Clean Code, OOP, SOLID, 정석적 해결)
4. **풍부한 도메인 모델** — Entity에 비즈니스 로직
5. **트러블슈팅 발생 시** → `docs/` 폴더에 .md 기록

## 모든 Phase 공통 — 반드시 읽을 문서

> **어떤 Phase든 작업 시작 전에 아래 2개는 무조건 먼저 읽는다.**

```
CLAUDE.md      → 프로젝트 개요, 빌드 명령어, 아키텍처 요약, 코딩 규칙 요약
AGENTS.md      → 코딩 규칙 16개 (0번 학습 의무 + 1~15번 규칙 전체)
```

각 Phase의 "읽을 문서"는 위 2개에 **추가로** 읽어야 하는 것들이다.

---

## Phase 1 — 기반 세팅

> 데모: Docker Compose up → 모든 인프라 정상 기동 확인

### 읽을 문서
```
BUILD_GRADLE_DESIGN.md     → 멀티 모듈 의존성
DOCKER_COMPOSE_DESIGN.md   → 서비스 구성, 포트, 프로필
APPLICATION_YML_DESIGN.md  → 설정 파일 구조
PACKAGE_STRUCTURE.md       → 모듈/패키지 뼈대
DB_INIT_SCRIPTS.md         → PostgreSQL/Neo4j 초기화
```

### 할 일
- [ ] 모노레포 구조 생성
- [ ] Gradle 멀티 모듈 세팅 (core, api, collector, ai, automation)
- [ ] Docker Compose 작성 (Neo4j, PostgreSQL, Redis, Elasticsearch, Kafka, n8n)
- [ ] Spring Boot 기본 설정 + 각 DB 연결 확인
- [ ] `.env` + `spring-dotenv` 환경변수 구성
- [ ] Health Check API (`/actuator/health`)

### 결과물
```
docker-compose up → Neo4j, PostgreSQL, Redis, ES, Kafka, n8n 전부 기동
Spring Boot 앱 → 각 DB 연결 성공 로그
```

---

## Phase 2 — 인증 + 결제

> 데모: 회원가입 → 로그인 → 구독 결제 → 플랜별 기능 제한 동작

### 읽을 문서
```
AUTH_PAYMENT.md            → 인증(JWT) + 결제(Stripe/Toss) + 구독 플랜 + API 키
DB_INIT_SCRIPTS.md         → users, subscriptions, payments 테이블
DB_OPTIMIZATION.md         → PostgreSQL 인덱스, 파티셔닝, 커넥션 풀
TRANSACTION_DESIGN.md      → Tier 1 (결제 보상 트랜잭션)
SEQUENCE_DIAGRAMS.md       → 결제 흐름, 로그인 + JWT 갱신 시퀀스
API_ENDPOINTS.md           → 인증 5개 + 결제 7개 + 유저 4개 API
API_JSON_EXAMPLES.md       → 인증/결제 응답 형식
PACKAGE_STRUCTURE.md       → account 도메인 패키지 위치
```

### 할 일
- [ ] User Entity + Repository (PostgreSQL)
- [ ] 회원가입/로그인 API (이메일 + 비밀번호 + JWT)
- [ ] Spring Security 설정
- [ ] Subscription Entity + 플랜 (Free/Pro/Enterprise)
- [ ] PaymentGateway 인터페이스 + Strategy 패턴
- [ ] Stripe 연동 (테스트 모드)
- [ ] Toss Payments 연동 (테스트 모드)
- [ ] PaymentGatewayRouter (통화 기반 분기)
- [ ] Stripe/Toss 웹훅 수신 엔드포인트
- [ ] API 사용량 카운터 (Redis INCR + 자정 리셋)
- [ ] 플랜별 기능 제한 체크 (SubscriptionDomainService)
- [ ] 프로필 수정 / 비밀번호 변경 / 계정 탈퇴 API
- [ ] 외부 API 키 관리 (Enterprise) — 발급, 조회, 재발급
- [ ] 감사 로그 기록 (로그인, 주요 액션)

### 결과물
```
POST /api/auth/signup → 회원가입
POST /api/auth/login → JWT 발급
POST /api/payments/subscribe → Stripe/Toss 결제
GET /api/ai/query → Free 플랜 5건 초과 시 403
POST /api/external/api-key/regenerate → Enterprise API 키 발급
GET /api/audit/logs → 감사 로그 조회
```

---

## Phase 3 — 데이터 수집

> 데모: 11개 소스에서 데이터 수집 → Kafka 적재 → Redis 위치 저장

### 읽을 문서
```
KAFKA_DESIGN.md            → 8개 토픽, 파티션, 메시지 포맷, Consumer Group
TECH_STACK.md              → WebClient, Playwright, AISstream
PROJECT_OVERVIEW.md        → 11개 데이터 소스 목록
PACKAGE_STRUCTURE.md       → collector 모듈 패키지 위치
DB_OPTIMIZATION.md         → Redis GEO, Elasticsearch 설정
```

### 할 일
- [ ] AISstream WebSocket 클라이언트 → Kafka Producer
- [ ] Open-Meteo REST 수집기 (날씨/태풍)
- [ ] UN 제재 목록 수집기 (XML 파싱 또는 OpenSanctions JSON)
- [ ] GNews 수집기 (뉴스)
- [ ] EIA 수집기 (유가)
- [ ] Frankfurter 수집기 (환율)
- [ ] GDELT 수집기 (지정학 리스크)
- [ ] UN/LOCODE 수집기 (항만 정보, 벌크 로드)
- [ ] Marine Regions 수집기 (EEZ GeoJSON, 벌크 로드)
- [ ] Natural Earth/OSM 수집기 (초크포인트 좌표, 벌크 로드)
- [ ] UNCTAD Port Tracker 크롤러 (항만 혼잡도)
- [ ] Kafka Consumer → Redis GEO (선박 위치)
- [ ] Kafka Consumer → Elasticsearch (뉴스)
- [ ] 수집 스케줄러 (각 API별 주기 설정)

### 결과물
```
AIS WebSocket → Kafka → Redis GEO에 선박 위치 저장
GEORADIUS vessels:positions 129.0 35.1 100 km → 부산 근처 선박 목록
Elasticsearch → 뉴스 전문 검색 동작
```

---

## Phase 4 — 온톨로지 엔진

> 데모: Neo4j에서 4홉 Cypher 쿼리 → 제재 선박 탐지

### 읽을 문서
```
ARCHITECTURE.md            → 온톨로지 모델, 노드/관계 정의
DB_OPTIMIZATION.md         → Neo4j 인덱스, 4홉 쿼리 최적화
DB_INIT_SCRIPTS.md         → Neo4j 초기화 Cypher (초크포인트, 항로)
PACKAGE_STRUCTURE.md       → core 모듈 maritime 도메인 패키지
API_ENDPOINTS.md           → 온톨로지 8개 + 선박 13개 + 항만 8개 API
```

### 할 일
- [ ] Neo4j 노드/관계 모델 정의 (Vessel, Port, Company, Country, Route, Chokepoint, SeaZone, Sanction, EEZ, WeatherCondition)
- [ ] Spring Data Neo4j 엔티티 매핑
- [ ] 데이터 파이프라인: Raw → Clean → Ontology 자동 매핑
- [ ] Vessel-Port, Vessel-Company, Route-Chokepoint 관계 자동 생성
- [ ] 4홉 Cypher 쿼리 구현
- [ ] 인덱스 생성 (IMO, MMSI, LOCODE, ISO 등)
- [ ] 온톨로지 조회 API (그래프, 노드 펼침, 엔티티 검색)
- [ ] 두 엔티티 간 최단 경로 API
- [ ] 제재 네트워크 그래프 API
- [ ] 회사별/국적별 선박 조회 API
- [ ] 온톨로지 통계 API (노드 수, 관계 수)
- [ ] 글로벌 검색 + 자동완성

### 결과물
```
Neo4j Browser → 온톨로지 그래프 시각화
GET /api/ontology/graph/{nodeId}/expand → 노드 클릭 → 관계 펼침
GET /api/ontology/path?from=X&to=Y → 최단 경로
GET /api/search?q=EVER GIVEN → 통합 검색 결과
```

---

## Phase 5 — AI 의사결정

> 데모: 자연어 질의 → AI 판단 + 리스크 스코어 → 응답

### 읽을 문서
```
ARCHITECTURE.md            → AI 의사결정 시나리오, Domain Service 목록
SEQUENCE_DIAGRAMS.md       → 자연어 AI 질의, What-if 시뮬레이션 흐름
TRANSACTION_DESIGN.md      → Tier 2 (최종 일관성), Tier 3 (AI 캐싱)
API_ENDPOINTS.md           → AI 13개 + 리스크 14개 + 이상탐지 5개 API
API_JSON_EXAMPLES.md       → AI 질의/브리핑/What-if 응답 방향
PACKAGE_STRUCTURE.md       → ai 모듈 + intelligence 도메인 패키지
```

### 할 일
- [ ] OpenClaw 클라이언트 구현
- [ ] 리스크 스코어링 엔진 (0~100) — 선박/항로/초크포인트/항만별
- [ ] 자연어 → Cypher 변환 (LLM이 Neo4j 쿼리 생성)
- [ ] 자동 브리핑 생성 (매일 아침 리스크 서머리)
- [ ] What-if 시뮬레이션 ("수에즈 봉쇄 시 영향?")
- [ ] 시뮬레이션 이력/결과 저장 + 템플릿
- [ ] AI 판단 로그 → Elasticsearch 저장
- [ ] AI 추천 액션 목록 + 실행 기능
- [ ] AI 리포트 생성 (PDF)
- [ ] AI 사용량 조회 API
- [ ] SupplyChainRiskDomainService 구현
- [ ] SanctionScreeningDomainService 구현
- [ ] RouteOptimizationDomainService 구현
- [ ] AnomalyDetectionDomainService 구현 (AIS 소실, 항로 이탈, 속도 이상, 다크 쉽)
- [ ] RouteComparisonDomainService 구현 (항로/항만 비교)
- [ ] ExportDomainService 구현 (PDF, CSV 내보내기)
- [ ] 리스크 추세/일일/주간 리포트 API

### 결과물
```
POST /api/ai/query → "호르무즈 근처 제재 선박?" → AI 자연어 응답
GET /api/ai/briefing → 오늘의 리스크 서머리
POST /api/ai/whatif → "수에즈 3일 봉쇄" → 영향 분석 결과
GET /api/anomaly/ais-off → AIS 소실 선박 목록
GET /api/risk/score/vessel/{imo} → 리스크 스코어 0~100
GET /api/export/risk-report/pdf → PDF 다운로드
POST /api/compare/routes → 항로 비교 결과
```

---

## Phase 6 — 자동화

> 데모: 리스크 이벤트 발생 → n8n 워크플로우 트리거 → Slack 알림

### 읽을 문서
```
KAFKA_DESIGN.md            → risk-alerts 토픽, Consumer Group 3개
SEQUENCE_DIAGRAMS.md       → 리스크 알림 전체 흐름
PACKAGE_STRUCTURE.md       → automation 모듈 + operation 도메인 패키지
API_ENDPOINTS.md           → 워크플로우 8개 + 알림 8개 API
DB_INIT_SCRIPTS.md         → workflows, notification_rules 테이��
```

### 할 일
- [ ] n8n Docker 설정 + Webhook 연동
- [ ] 리스크 이벤트 → Kafka → n8n Webhook 트리거
- [ ] Slack 알림 워크플로우
- [ ] Email 알림 워크플로우
- [ ] 이벤트 → AI 판단 → 액션 전체 흐름 파이프라인
- [ ] 타임라인 이력 저장 (Elasticsearch)
- [ ] 워크플로우 CRUD API (생성/조회/수정/삭제/활성화/비활성화)
- [ ] 워크플로우 실행 이력 API
- [ ] 알림 규칙 CRUD API (조건 + 채널 설정)
- [ ] 알림 설정 API (Slack/Email 채널 관리)
- [ ] 알림 발송 이력 + 테스트 발송 API

### 결과물
```
태풍 감지 이벤트 → AI 리스크 HIGH 판단 → n8n 트리거 → Slack 알림 도착
POST /api/workflows → 워크플로우 생성
POST /api/notifications/rules → "호르무즈 HIGH면 Slack 알림" 규칙 생성
GET /api/risk/timeline → 이벤트 → 판단 → 액션 전체 이력
```

---

## Phase 7 — 프론트엔드

> 데모: 팔란티어 감성 대시보드 전체 동작

### 읽을 문서
```
UI_DESIGN.md               → 5개 뷰 (지도, 그래프, 타임라인, 워룸, 질의 바)
TECH_STACK.md              → React, Mapbox, Cytoscape.js, Zustand, Axios 등
PACKAGE_STRUCTURE.md       → frontend 폴더 구조
API_ENDPOINTS.md           → 174개 전체 (프론트에서 호출할 API)
API_JSON_EXAMPLES.md       → 공통 응답 형식
AGENTS.md                  → 15번 백프론트 연결 규칙
```

### 할 일
- [ ] React 프로젝트 세팅
- [ ] 지도 뷰 (Mapbox/Leaflet + 선박 위치 + 히트맵 + 초크포인트 + EEZ)
- [ ] 그래프 뷰 (D3.js/Cytoscape.js + Neo4j 데이터)
- [ ] 타임라인 뷰 (이벤트 → 판단 → 액션 이력)
- [ ] 워룸 대시보드 (리스크 현황 + 알림 피드 + 초크포인트 상태)
- [ ] 대시보드 위젯 (통계 차트, 혼잡도 랭킹, 리스크 추세)
- [ ] 자연어 질의 바 (OpenClaw 연동)
- [ ] 로그인/회원가입 페이지
- [ ] 구독/결제 페이지
- [ ] 유저 프로필/설정 페이지
- [ ] 워크플로우 관리 페이지
- [ ] 알림 규칙 설정 페이지
- [ ] 관심 선박(Watchlist) + 즐겨찾기 페이지
- [ ] 항로/항만 비교 페이지
- [ ] 시장 데이터 (유가/환율 차트) 페이지
- [ ] 기상/태풍 정보 페이지
- [ ] 내보내기 (PDF/CSV 다운로드) 기능
- [ ] 글로벌 검색 + 자동완성
- [ ] 관리자 페이지
- [ ] WebSocket 연동 (선박 위치, 알림, 대시보드 실시간)

### 결과물
```
브라우저 → 팔란티어 감성 대시보드 (174개 API + 3 WebSocket 연동)
지도에서 선박 클릭 → 상세 + 리스크 + 그래프 탐색
자연어 질의 → AI 응답 → 지도/그래프 연동
워크플로우 생성 → 조건 설정 → 자동 알림
```

---

## Phase 8 — 테스트

> 데모: 도메인 로직 단위 테스트 + 통합 테스트 전부 통과

### 읽을 문서
```
ARCHITECTURE.md            → Aggregate Root, Domain Service 목록 (테스트 대상)
TECH_STACK.md              → JUnit 5, Testcontainers, MockMvc
BUILD_GRADLE_DESIGN.md     → 테스트 의존성 (Testcontainers 모듈별)
```

### 설계 원칙

> 테스트 없으면 "그냥 돌아가게만 만든 거".
> 풍부한 도메인 모델의 장점은 **도메인 로직을 DB 없이 단위 테스트 가능**하다는 것.

### 할 일

**단위 테스트 (도메인 로직):**
- [ ] Vessel.evaluateRisk() — 제재국 선박 리스크 판단
- [ ] Route.passesThrough() — 초크포인트 통과 여부
- [ ] SupplyChainRiskDomainService — 다중 Aggregate 조합 판단
- [ ] SanctionScreeningDomainService — 제재 목록 대조
- [ ] RouteOptimizationDomainService — 대체 항로 추천
- [ ] SubscriptionDomainService — 플랜별 기능 제한
- [ ] PaymentGatewayRouter — 통화별 게이트웨이 분기

**통합 테스트:**
- [ ] Neo4j 4홉 쿼리 정합성 (Testcontainers)
- [ ] PostgreSQL 유저/결제 CRUD (Testcontainers)
- [ ] Redis GEO 공간 검색 (Testcontainers)
- [ ] Kafka Producer/Consumer 이벤트 흐름 (Testcontainers)
- [ ] API 엔드포인트 전체 (MockMvc)

### 결과물
```
./gradlew test → 전체 통과
단위 테스트: 도메인 로직 검증 (DB 없이 빠르게)
통합 테스트: Testcontainers로 실제 DB 환경 검증
```

---

## Phase 9 — 부하 테스트

> 데모: k6 부하 테스트 결과 → 성능 병목 분석 + 최적화 근거

### 읽을 문서
```
DB_OPTIMIZATION.md         → 커넥션 풀, 인덱스, 캐싱 전략 (병목 분석 기준)
KAFKA_DESIGN.md            → Consumer Lag 모니터링
```

### 할 일
- [ ] k6 설치 + 테스트 스크립트 작성
- [ ] 시나리오 1: AIS 데이터 대량 유입 (초당 수백 건)
- [ ] 시나리오 2: 4홉 Cypher 쿼리 동시 요청
- [ ] 시나리오 3: AI 질의 동시 요청
- [ ] 시나리오 4: Redis GEO 공간 검색 동시 요청
- [ ] 결과 분석: 응답 시간, p95, 처리량, 병목 지점
- [ ] 병목 발견 시 최적화 → 재테스트
- [ ] `docs/LOAD_TEST_RESULTS.md` 작성

### 결과물
```
| 시나리오 | 평균 응답 | p95 | 처리량 |
|---------|----------|-----|-------|
| AIS 유입 | Xms | Xms | X/sec |
| 4홉 쿼리 | Xms | Xms | X/sec |
| AI 질의 | Xms | Xms | X/sec |
| GEO 검색 | Xms | Xms | X/sec |
```

---

## Phase 10 — 모니터링

> 데모: Grafana 대시보드에서 시스템 상태 실시간 확인

### 읽을 문서
```
DOCKER_COMPOSE_DESIGN.md   → Prometheus + Grafana 서비스 설정
TECH_STACK.md              → Micrometer, Actuator
KAFKA_DESIGN.md            → 모니터링 지표 (Consumer Lag, Throughput)
```

### 할 일
- [ ] Prometheus + Grafana Docker Compose 추가
- [ ] Spring Boot Actuator + Micrometer → Prometheus 메트릭 노출
- [ ] Grafana 대시보드 구성:
  - JVM 메트릭 (힙, GC, 스레드)
  - DB 커넥션 풀 사용량 (HikariCP, Neo4j, Redis, ES)
  - Kafka Consumer lag
  - API 응답 시간 / 에러율
  - AIS 이벤트 처리량 (초당 건수)
- [ ] 알림 규칙 (커넥션 풀 80% 초과, 에러율 5% 초과 등)

### 결과물
```
Grafana → http://localhost:3000
대시보드: AnchorIQ System Overview
실시간 메트릭 + 알림 규칙 동작
```

---

## Phase 11 — 트러블슈팅 문서화

> 전 Phase에 걸쳐 진행 — 문제 발생 시 즉시 기록

### 읽을 문서
```
AGENTS.md                  → 7번 트러블슈팅 기록 원칙
DESIGN_CONVERSATION_LOG.md → 문서화 스타일 참고
```

### 원칙
- 문제 해결 시 `docs/` 폴더에 SCREAMING_SNAKE_CASE.md로 기록
- 구조: **현상 → 원인 추적 과정 → 해결 → CS 원리 → 교훈**
- 단순 해결법만 적지 않음, "왜 이런 현상이 일어나는지" 내부 동작 원리까지

### 예상 문서
```
docs/
  TROUBLESHOOTING_NEO4J_4HOP_PERFORMANCE.md
  TROUBLESHOOTING_AIS_WEBSOCKET_RECONNECT.md
  TROUBLESHOOTING_KAFKA_CONSUMER_LAG.md
  TROUBLESHOOTING_REDIS_GEO_MEMORY.md
  TROUBLESHOOTING_UNCTAD_CRAWLING_BLOCKED.md
  ...
```

### 왜 중요한가
- GitHub AI 분석 시 `docs/` 폴더 참조 → 포트폴리오 품질 직결
- 면접에서 "이 문제 어떻게 해결했어요?" → .md 보여주며 설명
- "AI가 다 해줬네" 의심 차단 → 문제 해결 과정이 기록으로 증명됨
