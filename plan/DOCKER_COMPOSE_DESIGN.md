# AnchorIQ — Docker Compose 설계

> 11개 서비스 원클릭 실행 — 프로필로 단계별 기동

---

## 목차
- [서비스 목록](#서비스-목록)
- [포트 매핑](#포트-매핑)
- [프로필 설계](#프로필-설계)
- [서비스별 설정](#서비스별-설정)
- [볼륨 설계](#볼륨-설계)
- [네트워크 설계](#네트워크-설계)
- [환경변수](#환경변수)
- [헬스체크](#헬스체크)
- [기동 순서](#기동-순서)
- [메모리 할당](#메모리-할당)

---

## 서비스 목록

| # | 서비스 | 이미지 | 역할 |
|---|--------|--------|------|
| 1 | postgresql | postgres:16 | 유저/결제/구독 |
| 2 | neo4j | neo4j:5-community | 온톨로지 그래프 |
| 3 | redis | redis:7-alpine | 캐싱 + GEO |
| 4 | elasticsearch | elasticsearch:8.13.0 | 뉴스/로그 검색 |
| 5 | kafka | confluentinc/cp-kafka:7.6.0 | 이벤트 스트리밍 (KRaft) |
| 6 | n8n | n8nio/n8n:latest | 워크플로우 자동화 |
| 7 | prometheus | prom/prometheus:latest | 메트릭 수집 |
| 8 | grafana | grafana/grafana:latest | 모니터링 대시보드 |
| 9 | anchoriq-app | 직접 빌드 | Spring Boot 앱 |
| 10 | anchoriq-frontend | 직접 빌드 | React 대시보드 |

> Zookeeper 제거 — Kafka KRaft 모드로 단독 실행 (메모리 ~500MB 절약)

---

## 포트 매핑

| 서비스 | 내부 포트 | 외부 포트 | 접속 URL |
|--------|----------|----------|---------|
| PostgreSQL | 5432 | 5432 | `jdbc:postgresql://localhost:5432/anchoriq` |
| Neo4j Browser | 7474 | 7474 | `http://localhost:7474` |
| Neo4j Bolt | 7687 | 7687 | `bolt://localhost:7687` |
| Redis | 6379 | 6379 | `redis://localhost:6379` |
| Elasticsearch | 9200 | 9200 | `http://localhost:9200` |
| Kafka | 9092 | 9092 | `localhost:9092` |
| n8n | 5678 | 5678 | `http://localhost:5678` |
| Prometheus | 9090 | 9090 | `http://localhost:9090` |
| Grafana | 3000 | 3001 | `http://localhost:3001` |
| Spring Boot | 8080 | 8080 | `http://localhost:8080` |
| React Dev | 3000 | 3000 | `http://localhost:3000` |

> Grafana 3001로 변경 — React Dev Server 3000과 충돌 방지

---

## 프로필 설계

> 개발 Phase에 따라 필요한 서비스만 기동 — 메모리 절약

### 프로필 구성

| 프로필 | 서비스 | 메모리 | Phase |
|--------|--------|--------|-------|
| `core` | PostgreSQL, Redis | ~500MB | Phase 1~2 |
| `data` | + Kafka, Elasticsearch | ~3GB | Phase 3 |
| `ontology` | + Neo4j | ~4GB | Phase 4 |
| `automation` | + n8n | ~4.3GB | Phase 5~6 |
| `frontend` | + React Dev | ~4.8GB | Phase 7 |
| `monitoring` | + Prometheus, Grafana | ~5.5GB | Phase 10 |
| `full` | 전부 | ~6GB | 최종 데모 |

### 사용법

```bash
# Phase 1~2: 인증 + 결제 개발
docker compose --profile core up -d

# Phase 3: 데이터 수집 추가
docker compose --profile core --profile data up -d

# Phase 4: 온톨로지 추가
docker compose --profile core --profile data --profile ontology up -d

# 최종 데모: 전부
docker compose --profile full up -d
```

---

## 서비스별 설정

### PostgreSQL

```yaml
postgresql:
  image: postgres:16
  profiles: ["core", "full"]
  environment:
    POSTGRES_DB: anchoriq
    POSTGRES_USER: ${DB_USERNAME}
    POSTGRES_PASSWORD: ${DB_PASSWORD}
  ports:
    - "5432:5432"
  volumes:
    - pg-data:/var/lib/postgresql/data
    - ./infra/sql/init.sql:/docker-entrypoint-initdb.d/init.sql
  deploy:
    resources:
      limits:
        memory: 256M
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME}"]
    interval: 10s
    timeout: 5s
    retries: 5
```

### Neo4j

```yaml
neo4j:
  image: neo4j:5-community
  profiles: ["ontology", "full"]
  environment:
    NEO4J_AUTH: ${NEO4J_USERNAME}/${NEO4J_PASSWORD}
    NEO4J_dbms_memory_heap_max__size: 512m
    NEO4J_dbms_memory_pagecache_size: 256m
  ports:
    - "7474:7474"
    - "7687:7687"
  volumes:
    - neo4j-data:/data
  deploy:
    resources:
      limits:
        memory: 1024M
  healthcheck:
    test: ["CMD", "neo4j", "status"]
    interval: 10s
    timeout: 5s
    retries: 5
```

### Redis

```yaml
redis:
  image: redis:7-alpine
  profiles: ["core", "full"]
  command: >
    redis-server
    --maxmemory 256mb
    --maxmemory-policy allkeys-lru
    --requirepass ${REDIS_PASSWORD}
  ports:
    - "6379:6379"
  volumes:
    - redis-data:/data
  deploy:
    resources:
      limits:
        memory: 300M
  healthcheck:
    test: ["CMD", "redis-cli", "-a", "${REDIS_PASSWORD}", "ping"]
    interval: 10s
    timeout: 5s
    retries: 5
```

### Elasticsearch

```yaml
elasticsearch:
  image: elasticsearch:8.13.0
  profiles: ["data", "full"]
  environment:
    - discovery.type=single-node
    - xpack.security.enabled=false
    - ES_JAVA_OPTS=-Xms512m -Xmx512m
  ports:
    - "9200:9200"
  volumes:
    - es-data:/usr/share/elasticsearch/data
  deploy:
    resources:
      limits:
        memory: 1536M
  healthcheck:
    test: ["CMD-SHELL", "curl -f http://localhost:9200/_cluster/health || exit 1"]
    interval: 15s
    timeout: 10s
    retries: 5
```

> nori 플러그인은 Dockerfile로 커스텀 이미지 빌드하거나 init 컨테이너로 설치

### Kafka (KRaft 모드)

```yaml
kafka:
  image: confluentinc/cp-kafka:7.6.0
  profiles: ["data", "full"]
  environment:
    KAFKA_NODE_ID: 1
    KAFKA_PROCESS_ROLES: broker,controller
    KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
    KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093,EXTERNAL://0.0.0.0:29092
    KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092,EXTERNAL://localhost:29092
    KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT,EXTERNAL:PLAINTEXT
    KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
    KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
    KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    KAFKA_LOG_RETENTION_HOURS: 1
    CLUSTER_ID: "anchoriq-kafka-cluster-001"
  ports:
    - "29092:29092"
  volumes:
    - kafka-data:/var/lib/kafka/data
  deploy:
    resources:
      limits:
        memory: 1024M
  healthcheck:
    test: ["CMD-SHELL", "kafka-broker-api-versions --bootstrap-server localhost:9092 || exit 1"]
    interval: 15s
    timeout: 10s
    retries: 5
```

### n8n

```yaml
n8n:
  image: n8nio/n8n:latest
  profiles: ["automation", "full"]
  environment:
    - N8N_BASIC_AUTH_ACTIVE=true
    - N8N_BASIC_AUTH_USER=${N8N_USER}
    - N8N_BASIC_AUTH_PASSWORD=${N8N_PASSWORD}
    - WEBHOOK_URL=http://localhost:5678/
  ports:
    - "5678:5678"
  volumes:
    - n8n-data:/home/node/.n8n
  deploy:
    resources:
      limits:
        memory: 256M
```

### Prometheus

```yaml
prometheus:
  image: prom/prometheus:latest
  profiles: ["monitoring", "full"]
  ports:
    - "9090:9090"
  volumes:
    - ./infra/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
    - prometheus-data:/prometheus
  deploy:
    resources:
      limits:
        memory: 512M
```

### Grafana

```yaml
grafana:
  image: grafana/grafana:latest
  profiles: ["monitoring", "full"]
  environment:
    - GF_SECURITY_ADMIN_USER=${GRAFANA_USER}
    - GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_PASSWORD}
  ports:
    - "3001:3000"
  volumes:
    - grafana-data:/var/lib/grafana
    - ./infra/grafana/dashboards:/etc/grafana/provisioning/dashboards
    - ./infra/grafana/datasources:/etc/grafana/provisioning/datasources
  deploy:
    resources:
      limits:
        memory: 256M
  depends_on:
    - prometheus
```

### Spring Boot App

```yaml
anchoriq-app:
  build:
    context: ./backend
    dockerfile: Dockerfile
  profiles: ["full"]
  environment:
    - SPRING_PROFILES_ACTIVE=docker
  env_file:
    - .env
  ports:
    - "8080:8080"
  deploy:
    resources:
      limits:
        memory: 1024M
  depends_on:
    postgresql:
      condition: service_healthy
    redis:
      condition: service_healthy
    neo4j:
      condition: service_healthy
    kafka:
      condition: service_healthy
    elasticsearch:
      condition: service_healthy
```

### React Frontend

```yaml
anchoriq-frontend:
  build:
    context: ./frontend
    dockerfile: Dockerfile.dev
  profiles: ["frontend", "full"]
  ports:
    - "3000:3000"
  volumes:
    - ./frontend:/app
    - /app/node_modules
  deploy:
    resources:
      limits:
        memory: 512M
  depends_on:
    - anchoriq-app
```

---

## 볼륨 설계

```yaml
volumes:
  pg-data:          # PostgreSQL 영구 데이터
  neo4j-data:       # Neo4j 그래프 데이터
  redis-data:       # Redis RDB 스냅샷
  es-data:          # Elasticsearch 인덱스 데이터
  kafka-data:       # Kafka 로그 세그먼트
  n8n-data:         # n8n 워크플로우 데이터
  prometheus-data:  # Prometheus 시계열 데이터
  grafana-data:     # Grafana 대시보드/설정
```

> 모든 데이터 볼륨은 Named Volume → `docker compose down`해도 데이터 유지
> 초기화하려면 `docker compose down -v` (볼륨까지 삭제)

---

## 네트워크 설계

```yaml
networks:
  anchoriq-net:
    driver: bridge
```

> 모든 서비스가 같은 네트워크 → 서비스명으로 통신
> 예: Spring Boot에서 `postgresql:5432`, `neo4j:7687`, `redis:6379`로 접근

---

## 환경변수

### .env 파일 (절대 커밋 금지)

```properties
# PostgreSQL
DB_USERNAME=anchoriq
DB_PASSWORD=anchoriq_dev_2026

# Neo4j
NEO4J_USERNAME=neo4j
NEO4J_PASSWORD=neo4j_dev_2026

# Redis
REDIS_PASSWORD=redis_dev_2026

# n8n
N8N_USER=admin
N8N_PASSWORD=n8n_dev_2026

# Grafana
GRAFANA_USER=admin
GRAFANA_PASSWORD=grafana_dev_2026

# JWT
JWT_SECRET=your-jwt-secret-key-here
JWT_ACCESS_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000

# Stripe
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...

# Toss Payments
TOSS_SECRET_KEY=test_sk_...
TOSS_WEBHOOK_SECRET=...

# OpenClaw
OPENCLAW_API_KEY=your-openclaw-key
OPENCLAW_BASE_URL=https://...

# AISstream
AISSTREAM_API_KEY=your-aisstream-key

# EIA
EIA_API_KEY=your-eia-key

# GNews
GNEWS_API_KEY=your-gnews-key

# API Key Secret
API_KEY_SECRET=your-api-key-secret
```

---

## 헬스체크

| 서비스 | 체크 방식 | 간격 | 타임아웃 | 재시도 |
|--------|----------|------|---------|--------|
| PostgreSQL | `pg_isready` | 10초 | 5초 | 5회 |
| Neo4j | `neo4j status` | 10초 | 5초 | 5회 |
| Redis | `redis-cli ping` | 10초 | 5초 | 5회 |
| Elasticsearch | `curl /_cluster/health` | 15초 | 10초 | 5회 |
| Kafka | `kafka-broker-api-versions` | 15초 | 10초 | 5회 |

> Spring Boot 앱은 모든 DB healthy 후에 시작 (depends_on + condition)

---

## 기동 순서

```
1단계: PostgreSQL, Redis, Neo4j (DB 레이어)
         ↓ healthy
2단계: Kafka, Elasticsearch (메시징 + 검색)
         ↓ healthy
3단계: n8n (자동화)
         ↓
4단계: anchoriq-app (Spring Boot)
         ↓
5단계: anchoriq-frontend (React)
         ↓
6단계: Prometheus, Grafana (모니터링)
```

> depends_on + healthcheck으로 자동 보장

---

## 메모리 할당

| 서비스 | 메모리 제한 | 근거 |
|--------|-----------|------|
| PostgreSQL | 256MB | 소규모 테이블, 충분 |
| Neo4j | 1024MB | heap 512MB + pagecache 256MB + 오버헤드 |
| Redis | 300MB | maxmemory 256MB + 프로세스 오버헤드 |
| Elasticsearch | 1536MB | JVM 512MB + Lucene 캐시 |
| Kafka | 1024MB | KRaft 모드, 파티션 11개 |
| n8n | 256MB | Node.js 경량 |
| Prometheus | 512MB | 2주 메트릭 보관 |
| Grafana | 256MB | 대시보드 렌더링 |
| Spring Boot | 1024MB | 멀티 모듈 + Kafka Consumer |
| React Dev | 512MB | Vite dev server |
| **총합** | **6.7GB** | **32GB 머신에서 12GB 할당 시 여유** |

---

## 인프라 디렉토리 구조

```
/infra/
  docker-compose.yml
  .env                          ← .gitignore 포함
  .env.example                  ← 커밋 가능, 키 값 비워둠
  /sql/
    init.sql                    ← PostgreSQL 초기화 (파티셔닝 등)
  /elasticsearch/
    Dockerfile                  ← nori 플러그인 설치용 커스텀 이미지
  /prometheus/
    prometheus.yml              ← 스크레이프 설정
  /grafana/
    /dashboards/
      anchoriq-overview.json    ← 미리 만든 대시보드
    /datasources/
      prometheus.yml            ← Prometheus 데이터소스 자동 등록
```
