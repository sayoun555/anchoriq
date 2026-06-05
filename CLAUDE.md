# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AnchorIQ — 선박/해운 공급망 리스크 탐지 및 액션 자동화 플랫폼. 팔란티어 Foundry/Gotham 스타일 포트폴리오 프로젝트. 11개 무료 API 데이터를 온톨로지(Neo4j)로 융합하고, AI(OpenClaw)가 리스크를 판단하여 n8n으로 자동 액션을 실행한다.

## 최우선 규칙: 설계 문서 학습 의무

> **코드 한 줄 쓰기 전에 설계 문서를 학습한다. 예외 없음.** 모든 설계 문서는 `plan/`에 있다(루트 아님).
>
> 1. `plan/AGENTS.md` — 코딩 규칙 + 0번 학습 의무 + 구현 체크리스트
> 2. `plan/IMPLEMENTATION_PLAN.md` — 현재 Phase 확인
> 3. 그 Phase가 지정한 `plan/` 문서 → 전부 읽은 뒤 코드 작성
>
> 학습 후 더 나은 방법으로 바꾸는 건 OK(근거 필수). 학습 없이 자기 방식은 금지.
> 이 절차는 `scripts/harness/` 훅으로 강제된다(아래 "개발 하네스").

## 설계 문서 (반드시 참조 — 전부 `plan/`)

| 문서 (`plan/`) | 내용 | 참조 시점 |
|------|------|----------|
| `AGENTS.md` | 코딩 규칙 15개 (Clean Code, OOP, SOLID, DDD, 트랜잭션, DB, Kafka 등) | **항상** |
| `ARCHITECTURE.md` | 온톨로지 모델, 데이터 파이프라인, DDD 설계, Aggregate Root, Domain Service | 구조 잡을 때 |
| `PACKAGE_STRUCTURE.md` | 5개 모듈 패키지 구조 (인터페이스 기반, 기능별 분리) | 파일/클래스 생성할 때 |
| `API_ENDPOINTS.md` | REST API + WebSocket 엔드포인트 | API 구현할 때 |
| `DB_OPTIMIZATION.md` | 4개 DB 최적화 + PostgreSQL 스키마 | DB 관련 작업 |
| `DB_INIT_SCRIPTS.md` | DB 초기화 스크립트 | DB 시딩/스키마 |
| `KAFKA_DESIGN.md` | 토픽, Consumer Group, 메시지 포맷, DLT | Kafka 관련 작업 |
| `TRANSACTION_DESIGN.md` | 3-Tier 트랜잭션 전략, 보상 트랜잭션, 동시성 | 트랜잭션 코드 작성 시 |
| `DOCKER_COMPOSE_DESIGN.md` | 서비스, 포트, 프로필, 메모리 | 인프라 설정 시 |
| `BUILD_GRADLE_DESIGN.md` | Gradle 멀티 모듈 의존성 | 의존성 추가/변경 시 |
| `APPLICATION_YML_DESIGN.md` | 설정 파일 3개 프로필 + .env | 설정 변경 시 |
| `TECH_STACK.md` | 기술 스택 상세 (버전, 라이브러리) | 라이브러리 추가 시 |
| `AUTH_PAYMENT.md` | 인증(JWT) + 결제(Stripe+Toss) + 구독 플랜 | 인증/결제 구현 시 |
| `UI_DESIGN.md` | 팔란티어 감성 대시보드 뷰 | 프론트 구현 시 |
| `IMPLEMENTATION_PLAN.md` | Phase별 구현 계획 | 작업 순서 확인 |
| `API_JSON_EXAMPLES.md` · `SEQUENCE_DIAGRAMS.md` · `DECISIONS.md` · `DESIGN_CONVERSATION_LOG.md` | 보조 참조 (예시/시퀀스/의사결정 기록) | 필요 시 |

## 개발 하네스 (HARNESS.md)

이 리포는 위 "설계 문서 학습 의무"를 **산문이 아니라 강제되는 훅**으로 구현해 둔 에이전트 하네스를 갖는다. 1페이지 요약은 `HARNESS.md`, 전체 문서는 `docs/harness-engineering/`(트러블슈팅 포함) 참조.

- `.claude/settings.json` (팀 공통, 커밋됨) — 4개 훅 + 권한 allow/deny
  - `SessionStart` → `scripts/harness/session-context.sh`: design-first 프로토콜 + `plan/` 문서 인덱스 + 열린 findings 주입
  - `PreToolUse(Edit|Write)` → `scripts/harness/governing-doc.sh`: 수정 파일을 지배하는 `plan/` 문서를 한 줄로 포인팅(코드 파일만)
  - `PostToolUse(Edit|Write)` → 의미 리뷰어(agent, `scripts/harness/review-protocol.md`): DDD/트랜잭션/계약 위반 검사 → 위반 시 반려 + DLT 적재
  - `Stop` → `scripts/harness/check-stubs.sh --hook`: 백엔드 main 소스의 미완성 마커(stub/TODO/mock/`// 임시`) 경고
- `.claude/settings.local.json` (개인, gitignore) — 개인 도구 권한만, 시크릿 금지
- findings DLT 원장: `scripts/harness/finding.sh list --open` / `resolve <id>` (열린 위반은 매 세션 재노출)
- CI/수동 품질 게이트: `bash scripts/harness/check-stubs.sh` (마커 발견 시 exit 1)
- 훅을 새로 추가했다면 `/hooks`를 한 번 열거나 재시작해야 라이브가 된다.

## Build & Run Commands

> 경로 주의: 리포지토리 루트가 곧 `anchoriq/`다. 명령은 루트 기준 `backend/`, `frontend/`, `infra/`를 사용한다 (`cd anchoriq/...` 아님).

### Backend (Spring Boot — Gradle 멀티 모듈)
```bash
# 빌드
cd backend && ./gradlew build

# 실행 (로컬 프로필 — http://localhost:8082)
cd backend && ./gradlew :anchoriq-api:bootRun --args='--spring.profiles.active=local'

# 실행 (Docker 프로필)
cd backend && ./gradlew :anchoriq-api:bootRun --args='--spring.profiles.active=docker'

# 테스트 전체
cd backend && ./gradlew test

# 단일 모듈 테스트
cd backend && ./gradlew :anchoriq-core:test

# 단일 테스트 클래스
cd backend && ./gradlew :anchoriq-core:test --tests "com.anchoriq.core.domain.intelligence.risk.service.SupplyChainRiskServiceImplTest"
```
포트: `local` 프로필은 **8082**, 기본(`application.yml`)은 `SERVER_PORT:8080`.

### Frontend (Next.js 16 — App Router)
```bash
cd frontend && pnpm install
cd frontend && pnpm dev          # 개발 서버 (next dev -p 3004 → localhost:3004)
cd frontend && pnpm build        # 프로덕션 빌드 (next build)
cd frontend && pnpm start        # 프로덕션 실행
cd frontend && pnpm lint         # ESLint (eslint-config-next)
```
- 백엔드 연결: `next.config.ts`의 `rewrites`가 `/api/*`·`/ws/*`를 `http://localhost:8082`로 프록시.
- `frontend/AGENTS.md` 규칙: 이 Next.js는 학습 데이터와 다를 수 있으니 코드 작성 전 `node_modules/next/dist/docs/`의 가이드를 읽을 것.
- **`frontend-vite/`는 마이그레이션 전 레거시(React+Vite) 프론트엔드**다. 신규 작업은 `frontend/`(Next.js)에서 한다.

### Docker Compose
```bash
# 프로필별 실행 (profiles: core, ontology, data, automation, monitoring, frontend, full)
cd infra && docker compose --profile core up -d                          # PostgreSQL + Redis
cd infra && docker compose --profile core --profile data up -d           # + Kafka + Elasticsearch
cd infra && docker compose --profile ontology up -d                      # Neo4j
cd infra && docker compose --profile full up -d                          # 전체 서비스

# 종료 / 볼륨 포함 초기화
cd infra && docker compose down
cd infra && docker compose down -v
```

### 부하 테스트 (k6)
```bash
k6 run backend/k6/ais-load-test.js        # AIS WebSocket
k6 run backend/k6/api-load-test.js        # REST API
k6 run backend/k6/neo4j-load-test.js      # Neo4j 그래프 질의
k6 run backend/k6/redis-geo-load-test.js  # Redis GEO 공간검색
```

## Architecture

모노레포: `backend/`(Spring Boot 멀티 모듈) + `frontend/`(Next.js 16) + `infra/`(Docker) + `docs/`(트러블슈팅 기록) + `plan/`(설계 문서) + `frontend-vite/`(레거시)

프론트 구조(`frontend/src/`): `app/`(App Router — `(auth)`·`(dashboard)` 라우트 그룹), `api/`(백엔드 호출 모듈: auth/risk/map/ontology/vessels 등), `components/`, `features/`, `stores/`(Zustand), `contexts/`, `hooks/`, `types/`, `lib/`, `utils/`

### Backend 모듈 구조
```
anchoriq-core/        → 순수 도메인 (Entity, VO, Domain Service, Repository 인터페이스)
anchoriq-api/         → REST Controller, DTO, Security, 인프라 구현체 (실행 모듈)
anchoriq-collector/   → 11개 API 수집기, Playwright 크롤러, Kafka Producer/Consumer
anchoriq-ai/          → OpenClaw 연동, 리스크 스코어링, 자연어 질의, What-if
anchoriq-automation/  → n8n 연동, 알림 발송, 타임라인 관리
```

### 모듈 의존성 규칙
```
anchoriq-core → 외부 모듈 의존 금지 (순수 도메인)
anchoriq-api → anchoriq-core, anchoriq-ai
anchoriq-collector → anchoriq-core
anchoriq-ai → anchoriq-core
anchoriq-automation → anchoriq-core, anchoriq-ai
```

### DDD 레이어 (각 모듈 내부)
```
api/              → Controller (입력 검증, 응답 변환만)
application/      → Application Service (오케스트레이션만, 비즈니스 로직 X)
domain/model/     → Entity, VO, Aggregate Root (비즈니스 로직 보유)
domain/service/   → Domain Service 인터페이스 + 구현체
domain/repository/→ Repository 인터페이스 (구현은 infrastructure)
infrastructure/   → 외부 연동 (DB 구현체, API 클라이언트, Kafka)
```

### 도메인 그룹 (core 모듈)
```
domain/maritime/       → 해운 핵심 (vessel, port, route, company, country, sanction, weather, eez)
domain/intelligence/   → 리스크/AI (risk, anomaly)
domain/account/        → 유저/결제 (user, subscription, payment, apikey)
domain/operation/      → 운영 (workflow, notification, bookmark, audit)
```

### DB 4개
```
PostgreSQL  → 유저/결제/구독 (Tier 1 강한 일관성)
Neo4j       → 온톨로지 그래프 (Tier 2 최종 일관성)
Redis       → 실시간 캐싱 + GEO 공간 검색 (Tier 3)
Elasticsearch → 뉴스/로그 전문 검색 (Tier 3)
```

### 환경변수
- `spring-dotenv` 라이브러리로 `.env` 파일 로드
- `application.yml`에서 `${환경변수명:기본값}` 형태로 참조
- `.env`는 `.gitignore`에 포함, 절대 커밋 금지
- 프로필: `local`(IDE 실행), `docker`(Docker Compose), `test`(Testcontainers)

## Coding Rules (AGENTS.md 참조)

1. **Clean Code**: 의도 명확한 네이밍, 메서드 단일 책임, 주석 없이 이해되는 코드
2. **OOP**: 캡슐화, Tell Don't Ask, Getter/Setter 지양, 원시 타입 포장, 일급 컬렉션
3. **SOLID**: SRP (길어지면 즉시 분리), OCP (인터페이스+다형성), DIP (추상화 의존)
4. **DDD**: 풍부한 도메인 모델, Entity에 비즈니스 로직, Application Service는 오케스트레이션만
5. **인터페이스 기반**: 모든 Service, Repository, Gateway → 인터페이스 먼저 정의
6. **기능별 파일 분리**: 200줄 초과 예상 시 분리, 메서드 7개 초과 시 분리
7. **트랜잭션**: Tier 1(돈/유저) @Transactional, Tier 2(온톨로지) Kafka 최종 일관성, Tier 3(캐시) 실패 무시
8. **외부 API**: @Transactional 안에서 호출 금지 → 보상 트랜잭션 패턴
9. **환경변수**: 민감 정보 하드코딩 절대 금지, .env + spring-dotenv
10. **정석적 해결**: 임시방편/꼼수/우회 금지, 공식 권장 방식
11. **트러블슈팅**: 문제 해결 시 `docs/` 폴더에 SCREAMING_SNAKE_CASE.md로 기록
12. **백프론트 연결**: API 하나 만들면 프론트 호출 코드까지 완성해야 "완료"

## Tech Stack

- **Backend**: Java 21, Spring Boot 3.4, Spring Security, Spring Data JPA/Neo4j/Redis/Elasticsearch, Spring Kafka, Gradle 멀티 모듈
- **Frontend**: Next.js 16 (App Router), React 19, TypeScript, Tailwind CSS v4, shadcn/Radix UI, Leaflet(+react-leaflet-cluster)·Mapbox GL JS(지도), Cytoscape.js(온톨로지 그래프), Recharts(차트), STOMP/SockJS(WebSocket), Zustand, Axios, pnpm. 인증은 HttpOnly 쿠키
- **Database**: PostgreSQL 16, Neo4j 5 CE, Redis 7, Elasticsearch 8.13
- **Messaging**: Kafka (KRaft 모드, Zookeeper 없음)
- **AI**: OpenClaw (OpenAI 게이트웨이)
- **Automation**: n8n
- **Crawling**: Playwright 1.49
- **Payment**: Stripe + Toss Payments (Strategy 패턴)
- **Monitoring**: Prometheus + Grafana
- **Test**: JUnit 5, Testcontainers, k6
- **Infra**: Docker Compose
