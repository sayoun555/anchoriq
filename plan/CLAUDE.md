# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AnchorIQ — 선박/해운 공급망 리스크 탐지 및 액션 자동화 플랫폼. 팔란티어 Foundry/Gotham 스타일 포트폴리오 프로젝트. 11개 무료 API 데이터를 온톨로지(Neo4j)로 융합하고, AI(OpenClaw)가 리스크를 판단하여 n8n으로 자동 액션을 실행한다.

## 최우선 규칙: 설계 문서 학습 의무

> **코드 한 줄 작성하기 전에, 반드시 아래 순서를 따라라. 예외 없음.**

### 작업 시작 시 필수 절차

```
1단계: 이 파일(CLAUDE.md) 읽기 (자동)
2단계: AGENTS.md 전체 읽기 (특히 0번 학습 의무 + 구현 체크리스트)
3단계: IMPLEMENTATION_PLAN.md에서 현재 Phase 확인
4단계: 해당 Phase의 "읽을 문서" 목록 확인 → 전부 읽기
5단계: 그제서야 코드 작성
```

> 학습 후 더 나은 방법으로 변경하는 것은 OK (근거 필수).
> 학습 안 하고 자기 방식으로 구현하는 것은 금지.
> 이 절차를 건너뛰면 설계와 다른 코드가 나온다. 반드시 지켜라.

## 설계 문서 (반드시 참조)

구현 전 반드시 해당 설계 문서를 읽고 따를 것:

| 문서 | 내용 | 참조 시점 |
|------|------|----------|
| `AGENTS.md` | 코딩 규칙 15개 (Clean Code, OOP, SOLID, DDD, 트랜잭션, DB, Kafka 등) | **항상** |
| `ARCHITECTURE.md` | 온톨로지 모델, 데이터 파이프라인, DDD 설계, Aggregate Root, Domain Service | 구조 잡을 때 |
| `PACKAGE_STRUCTURE.md` | 5개 모듈 패키지 구조 (인터페이스 기반, 기능별 분리) | 파일/클래스 생성할 때 |
| `API_ENDPOINTS.md` | 174개 REST API + 3 WebSocket 엔드포인트 | API 구현할 때 |
| `DB_OPTIMIZATION.md` | 4개 DB 최적화 + PostgreSQL 스키마 11개 테이블 | DB 관련 작업 |
| `KAFKA_DESIGN.md` | 8개 토픽, Consumer Group, 메시지 포맷, DLT | Kafka 관련 작업 |
| `TRANSACTION_DESIGN.md` | 3-Tier 트랜잭션 전략, 보상 트랜잭션, 동시성 | 트랜잭션 코드 작성 시 |
| `DOCKER_COMPOSE_DESIGN.md` | 11개 서비스, 포트, 프로필, 메모리 | 인프라 설정 시 |
| `BUILD_GRADLE_DESIGN.md` | Gradle 멀티 모듈 의존성 | 의존성 추가/변경 시 |
| `APPLICATION_YML_DESIGN.md` | 설정 파일 3개 프로필 + .env | 설정 변경 시 |
| `TECH_STACK.md` | 기술 스택 상세 (버전, 라이브러리) | 라이브러리 추가 시 |
| `AUTH_PAYMENT.md` | 인증(JWT) + 결제(Stripe+Toss) + 구독 플랜 | 인증/결제 구현 시 |
| `UI_DESIGN.md` | 팔란티어 감성 대시보드 5개 뷰 | 프론트 구현 시 |
| `IMPLEMENTATION_PLAN.md` | 11 Phase 구현 계획 | 작업 순서 확인 |

## Build & Run Commands

### Backend (Spring Boot — Gradle 멀티 모듈)
```bash
# 빌드
cd anchoriq/backend && ./gradlew build

# 실행 (로컬 프로필)
cd anchoriq/backend && ./gradlew :anchoriq-api:bootRun --args='--spring.profiles.active=local'

# 실행 (Docker 프로필)
cd anchoriq/backend && ./gradlew :anchoriq-api:bootRun --args='--spring.profiles.active=docker'

# 테스트 전체
cd anchoriq/backend && ./gradlew test

# 단일 모듈 테스트
cd anchoriq/backend && ./gradlew :anchoriq-core:test

# 단일 테스트 클래스
cd anchoriq/backend && ./gradlew :anchoriq-core:test --tests "com.anchoriq.core.domain.intelligence.risk.service.SupplyChainRiskServiceImplTest"
```

### Frontend (React + Vite)
```bash
cd anchoriq/frontend && pnpm install
cd anchoriq/frontend && pnpm dev        # 개발 서버 (localhost:3000)
cd anchoriq/frontend && pnpm build      # 프로덕션 빌드
cd anchoriq/frontend && pnpm lint       # ESLint
```

### Docker Compose
```bash
# Phase별 실행
cd anchoriq/infra && docker compose --profile core up -d              # PostgreSQL + Redis
cd anchoriq/infra && docker compose --profile core --profile data up -d   # + Kafka + ES
cd anchoriq/infra && docker compose --profile full up -d              # 전체 11개 서비스

# 종료
cd anchoriq/infra && docker compose down

# 볼륨 포함 초기화
cd anchoriq/infra && docker compose down -v
```

### 부하 테스트 (k6)
```bash
k6 run anchoriq/backend/k6/ais-load-test.js
k6 run anchoriq/backend/k6/api-load-test.js
```

## Architecture

모노레포: `backend/`(Spring Boot 멀티 모듈) + `frontend/`(React) + `infra/`(Docker) + `docs/`(문서)

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
- **Frontend**: React 19, TypeScript, Vite, Tailwind CSS, shadcn/ui, Mapbox GL JS, Cytoscape.js, Zustand, Axios, pnpm
- **Database**: PostgreSQL 16, Neo4j 5 CE, Redis 7, Elasticsearch 8.13
- **Messaging**: Kafka (KRaft 모드, Zookeeper 없음)
- **AI**: OpenClaw (OpenAI 게이트웨이)
- **Automation**: n8n
- **Crawling**: Playwright 1.49
- **Payment**: Stripe + Toss Payments (Strategy 패턴)
- **Monitoring**: Prometheus + Grafana
- **Test**: JUnit 5, Testcontainers, k6
- **Infra**: Docker Compose
