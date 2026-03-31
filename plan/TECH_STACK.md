# AnchorIQ — 기술 스택 상세

> 사용할 언어, 프레임워크, 라이브러리, 버전 정리

---

## 목차
- [Backend](#backend)
- [Frontend](#frontend)
- [Database](#database)
- [Messaging](#messaging)
- [Automation](#automation)
- [Monitoring](#monitoring)
- [Infrastructure](#infrastructure)
- [외부 API / SDK](#외부-api--sdk)
- [테스트](#테스트)

---

## Backend

| 기술 | 버전 | 용도 |
|------|------|------|
| **Java** | 21 (LTS) | 메인 언어 |
| **Spring Boot** | 3.4.x | 프레임워크 |
| **Spring Security** | 6.x | 인증/인가 (JWT) |
| **Spring Data JPA** | 3.x | PostgreSQL ORM |
| **Spring Data Neo4j** | 7.x | Neo4j OGM |
| **Spring Data Redis** | 3.x | Redis 캐싱 + GEO |
| **Spring Data Elasticsearch** | 5.x | ES 연동 |
| **Spring Kafka** | 3.x | Kafka Producer/Consumer |
| **Spring WebSocket** | 6.x | STOMP over WebSocket |
| **Spring Actuator** | 3.x | 헬스체크 + 메트릭 |
| **Micrometer** | 1.x | Prometheus 메트릭 노출 |
| **spring-dotenv** | 4.x | .env 환경변수 로드 |
| **JJWT** | 0.12.x | JWT 토큰 생성/검증 |
| **Gradle** | 8.x | 빌드 도구 (멀티 모듈) |
| **Lombok** | 1.18.x | 보일러플레이트 제거 |
| **MapStruct** | 1.6.x | DTO ↔ Entity 매핑 |
| **Playwright** | 1.49.x | UNCTAD 크롤링 (브라우저 자동화) |
| **Spring WebClient** | 6.x | REST API 호출 (11개 외부 API, 비동기) |
| **OpenPDF** | 2.x | PDF 리포트 생성 (무료, LGPL) |
| **OpenCSV** | 5.x | CSV 내보내기 |
| **Swagger / SpringDoc** | 2.x | API 문서 자동 생성 |

---

## Frontend

| 기술 | 버전 | 용도 |
|------|------|------|
| **React** | 19.x | UI 프레임워크 |
| **TypeScript** | 5.x | 타입 안전성 |
| **Vite** | 6.x | 번들러 (Webpack 대비 빠름, 메모리 절약) |
| **React Router** | 7.x | 클라이언트 라우팅 |
| **Tailwind CSS** | 4.x | 스타일링 |
| **shadcn/ui** | latest | UI 컴포넌트 라이브러리 |
| **Mapbox GL JS** | 3.x | 지도 뷰 (선박 위치, 히트맵, 초크포인트) |
| **Cytoscape.js** | 3.x | 그래프 뷰 (온톨로지 시각화) |
| **Recharts** | 2.x | 차트 (리스크 추세, 유가, 환율) |
| **SockJS + @stomp/stompjs** | latest | WebSocket 연결 |
| **Axios** | 1.x | API 호출 (JWT 인터셉터) |
| **Zustand** | 5.x | 전역 상태 관리 |
| **React Query (TanStack)** | 5.x | 서버 상태 관리 + 캐싱 |
| **pnpm** | 9.x | 패키지 매니저 |

---

## Database

| 기술 | 버전 | 용도 | Docker 이미지 |
|------|------|------|-------------|
| **PostgreSQL** | 16 | 유저/결제/구독 | `postgres:16` |
| **Neo4j Community Edition** | 5.x | 온톨로지 그래프 | `neo4j:5-community` |
| **Redis** | 7.x | 캐싱 + GEO 공간 검색 | `redis:7-alpine` |
| **Elasticsearch** | 8.13.x | 뉴스/로그 전문 검색 | `elasticsearch:8.13.0` + nori 플러그인 |

---

## Messaging

| 기술 | 버전 | 용도 | Docker 이미지 |
|------|------|------|-------------|
| **Apache Kafka** | 7.6.x (Confluent) | 이벤트 스트리밍 | `confluentinc/cp-kafka:7.6.0` |

> KRaft 모드 — Zookeeper 제거 (메모리 ~500MB 절약)
> 8개 토픽, 최대 3 파티션

---

## Automation

| 기술 | 버전 | 용도 | Docker 이미지 |
|------|------|------|-------------|
| **n8n** | latest | 워크플로우 자동화 (Slack/Email 알림) | `n8nio/n8n:latest` |

---

## Monitoring

| 기술 | 버전 | 용도 | Docker 이미지 |
|------|------|------|-------------|
| **Prometheus** | latest | 메트릭 수집 | `prom/prometheus:latest` |
| **Grafana** | latest | 모니터링 대시보드 | `grafana/grafana:latest` |

---

## Infrastructure

| 기술 | 용도 |
|------|------|
| **Docker** | 컨테이너 |
| **Docker Compose** | 로컬 오케스트레이션 (11개 서비스) |
| **Git** | 버전 관리 (모노레포) |
| **GitHub** | 리포지토리 호스팅 |

---

## 외부 API / SDK

### 결제

| 기술 | 용도 |
|------|------|
| **Stripe Java SDK** | 글로벌 결제 (USD, 테스트 모드) |
| **Toss Payments REST API** | 한국 결제 (KRW, 테스트 모드) |

### AI

| 기술 | 용도 |
|------|------|
| **OpenClaw** (OpenAI 게이트웨이) | AI 의사결정, 자연어 질의, 브리핑, What-if |

### 데이터 수집 (11개)

| # | API | 방식 | SDK/라이브러리 |
|---|-----|------|--------------|
| 1 | AISstream.io | WebSocket | Java WebSocket Client |
| 2 | UN/LOCODE | 벌크 다운로드 | CSV 파싱 |
| 3 | UNCTAD Port Tracker | 크롤링 | Playwright (브라우저 자동화) |
| 4 | Open-Meteo | REST | WebClient |
| 5 | UN 제재 + OpenSanctions | XML/JSON | WebClient + JAXB |
| 6 | GNews | REST | WebClient |
| 7 | EIA | REST | WebClient |
| 8 | Frankfurter.app | REST | WebClient |
| 9 | GDELT | REST | WebClient |
| 10 | Marine Regions | GeoJSON 다운로드 | Jackson |
| 11 | Natural Earth / OSM | GeoJSON 다운로드 | Jackson |

---

## 테스트

| 기술 | 용도 |
|------|------|
| **JUnit 5** | 단위 테스트 |
| **Mockito** | 목 객체 |
| **AssertJ** | 가독성 높은 assertion |
| **Testcontainers** | 통합 테스트 (PostgreSQL, Neo4j, Redis, ES, Kafka 실제 컨테이너) |
| **MockMvc** | API 엔드포인트 테스트 |
| **k6** | 부하 테스트 |

---

## 버전 선택 기준

| 기준 | 설명 |
|------|------|
| Java 21 | 최신 LTS, Virtual Threads 사용 가능 |
| Spring Boot 3.4 | Java 21 완전 지원, 최신 안정 |
| PostgreSQL 16 | 최신 안정 |
| Neo4j 5 | Spring Data Neo4j 7과 호환 |
| Elasticsearch 8.13 | nori 플러그인 안정 지원 |
| React 19 | 최신 안정 |
| Vite 6 | Webpack 대비 빠르고 메모리 절약 (512MB → 256MB) |
