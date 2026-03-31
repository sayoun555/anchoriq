# AnchorIQ — 설정 파일 설계

> application.yml 프로필별 구성 — 민감 정보는 .env에서 로드

---

## 목차
- [프로필 구조](#프로필-구조)
- [application.yml (공통)](#applicationyml-공통)
- [application-local.yml (로컬 개발)](#application-localyml-로컬-개발)
- [application-docker.yml (Docker Compose)](#application-dockeryml-docker-compose)
- [application-test.yml (테스트)](#application-testyml-테스트)
- [.env 파일](#env-파일)

---

## 프로필 구조

```
/backend/anchoriq-api/src/main/resources/
  application.yml              ← 공통 설정 (프로필 무관)
  application-local.yml        ← 로컬 개발 (IDE에서 직접 실행)
  application-docker.yml       ← Docker Compose 환경
  application-test.yml         ← 테스트 (Testcontainers)
```

| 프로필 | 사용 시점 | DB 호스트 |
|--------|----------|----------|
| local | IDE에서 `./gradlew bootRun` | localhost |
| docker | Docker Compose로 실행 | 컨테이너명 (postgresql, neo4j...) |
| test | `./gradlew test` | Testcontainers (자동 할당) |

---

## application.yml (공통)

> 프로필 무관하게 공통 적용되는 설정

```yaml
spring:
  application:
    name: anchoriq

  # Java 21 Virtual Threads 활성화
  threads:
    virtual:
      enabled: true

  # 환경변수 로드 (.env)
  config:
    import: optional:file:.env[.properties]

  # JPA 공통
  jpa:
    open-in-view: false                      # OSIV 끄기 (성능 + 명시적 트랜잭션)
    hibernate:
      ddl-auto: validate                     # 스키마 자동 생성 금지 (flyway/수동 관리)
    properties:
      hibernate:
        default_batch_fetch_size: 100        # N+1 방지 배치 사이즈
        format_sql: true

  # Jackson
  jackson:
    serialization:
      write-dates-as-timestamps: false       # ISO 8601 날짜 포맷
    default-property-inclusion: non_null      # null 필드 제외
    time-zone: UTC

  # Kafka 공통
  kafka:
    consumer:
      auto-offset-reset: latest
      enable-auto-commit: false              # 수동 커밋
      max-poll-records: 500
      properties:
        spring.json.trusted.packages: com.anchoriq.*
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: 1
    listener:
      ack-mode: manual                       # 수동 커밋 모드

# JWT
jwt:
  secret: ${JWT_SECRET}
  access-expiration: ${JWT_ACCESS_EXPIRATION:900000}       # 15분
  refresh-expiration: ${JWT_REFRESH_EXPIRATION:604800000}  # 7일

# OpenClaw
openclaw:
  api-key: ${OPENCLAW_API_KEY}
  base-url: ${OPENCLAW_BASE_URL}
  timeout: 30000                             # 30초 (AI 응답 대기)

# AISstream
aisstream:
  api-key: ${AISSTREAM_API_KEY}
  websocket-url: wss://stream.aisstream.io/v0/stream

# 외부 API 키
external-api:
  eia-key: ${EIA_API_KEY}
  gnews-key: ${GNEWS_API_KEY}

# 결제
stripe:
  secret-key: ${STRIPE_SECRET_KEY}
  webhook-secret: ${STRIPE_WEBHOOK_SECRET}

toss:
  secret-key: ${TOSS_SECRET_KEY}
  webhook-secret: ${TOSS_WEBHOOK_SECRET}

# n8n
n8n:
  base-url: ${N8N_BASE_URL:http://localhost:5678}
  user: ${N8N_USER}
  password: ${N8N_PASSWORD}

# API Key
api-key:
  secret: ${API_KEY_SECRET}

# 수집 스케줄러 주기
collector:
  schedule:
    weather: "0 */10 * * * *"                # 10분마다
    sanction: "0 0 2 * * *"                  # 매일 새벽 2시
    news: "0 */15 * * * *"                   # 15분마다
    oil-price: "0 0 */6 * * *"               # 6시간마다
    exchange-rate: "0 0 */6 * * *"           # 6시간마다
    geopolitical: "0 */15 * * * *"           # 15분마다
    port-congestion: "0 */30 * * * *"        # 30분마다

# Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,prometheus,info
  endpoint:
    health:
      show-details: when_authorized
  metrics:
    tags:
      application: anchoriq

# Swagger
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    tags-sorter: alpha
    operations-sorter: alpha

# 로깅
logging:
  level:
    com.anchoriq: INFO
    org.springframework.data.neo4j: WARN
    org.hibernate.SQL: WARN
```

---

## application-local.yml (로컬 개발)

> IDE에서 직접 실행 — localhost로 DB 연결

```yaml
spring:
  profiles:
    active: local

  # PostgreSQL
  datasource:
    url: jdbc:postgresql://localhost:5432/anchoriq
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10
      connection-timeout: 3000
      idle-timeout: 600000

  # Neo4j
  neo4j:
    uri: bolt://localhost:7687
    authentication:
      username: ${NEO4J_USERNAME}
      password: ${NEO4J_PASSWORD}
    pool:
      max-connection-pool-size: 30

  # Redis
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD}
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5

  # Elasticsearch
  elasticsearch:
    uris: http://localhost:9200

  # Kafka
  kafka:
    bootstrap-servers: localhost:29092
    listener:
      concurrency: 3                         # ais-positions Consumer 병렬

  # JPA
  jpa:
    hibernate:
      ddl-auto: update                       # 로컬에서만 자동 스키마
    show-sql: true

# 로깅 (로컬은 디버그)
logging:
  level:
    com.anchoriq: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

---

## application-docker.yml (Docker Compose)

> Docker Compose 환경 — 컨테이너명으로 DB 연결

```yaml
spring:
  profiles:
    active: docker

  # PostgreSQL
  datasource:
    url: jdbc:postgresql://postgresql:5432/anchoriq
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10
      connection-timeout: 3000
      idle-timeout: 600000

  # Neo4j
  neo4j:
    uri: bolt://neo4j:7687
    authentication:
      username: ${NEO4J_USERNAME}
      password: ${NEO4J_PASSWORD}
    pool:
      max-connection-pool-size: 30

  # Redis
  data:
    redis:
      host: redis
      port: 6379
      password: ${REDIS_PASSWORD}
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5

  # Elasticsearch
  elasticsearch:
    uris: http://elasticsearch:9200

  # Kafka
  kafka:
    bootstrap-servers: kafka:9092
    listener:
      concurrency: 3

  # JPA
  jpa:
    hibernate:
      ddl-auto: validate                     # Docker에서는 스키마 수동 관리
```

---

## application-test.yml (테스트)

> Testcontainers — 실제 DB 컨테이너에서 테스트

```yaml
spring:
  profiles:
    active: test

  # Testcontainers가 자동으로 URL/포트 주입
  # 아래는 Testcontainers가 오버라이드함

  # PostgreSQL (Testcontainers)
  datasource:
    url: jdbc:tc:postgresql:16:///anchoriq_test
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver

  # JPA
  jpa:
    hibernate:
      ddl-auto: create-drop                  # 테스트마다 스키마 재생성
    show-sql: true

  # Kafka (테스트용 임베디드)
  kafka:
    bootstrap-servers: ${spring.embedded.kafka.brokers:localhost:9092}

# JWT (테스트용 고정 키)
jwt:
  secret: test-jwt-secret-key-for-unit-tests-only-do-not-use-in-production
  access-expiration: 900000
  refresh-expiration: 604800000

# OpenClaw (테스트용 Mock)
openclaw:
  api-key: test-key
  base-url: http://localhost:8089/mock

# 로깅
logging:
  level:
    com.anchoriq: DEBUG
    org.testcontainers: INFO
```

---

## .env 파일

> 절대 커밋 금지 — `.gitignore`에 포함

### .env (실제 값)

```properties
# PostgreSQL
DB_USERNAME=anchoriq
DB_PASSWORD=anchoriq_dev_2026

# Neo4j
NEO4J_USERNAME=neo4j
NEO4J_PASSWORD=neo4j_dev_2026

# Redis
REDIS_PASSWORD=redis_dev_2026

# JWT
JWT_SECRET=your-256-bit-jwt-secret-key-here-min-32-chars
JWT_ACCESS_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000

# OpenClaw
OPENCLAW_API_KEY=your-openclaw-api-key
OPENCLAW_BASE_URL=https://api.openclaw.example.com

# AISstream
AISSTREAM_API_KEY=your-aisstream-api-key

# EIA
EIA_API_KEY=your-eia-api-key

# GNews
GNEWS_API_KEY=your-gnews-api-key

# Stripe
STRIPE_SECRET_KEY=sk_test_xxxxx
STRIPE_WEBHOOK_SECRET=whsec_xxxxx

# Toss Payments
TOSS_SECRET_KEY=test_sk_xxxxx
TOSS_WEBHOOK_SECRET=xxxxx

# n8n
N8N_BASE_URL=http://localhost:5678
N8N_USER=admin
N8N_PASSWORD=n8n_dev_2026

# Grafana
GRAFANA_USER=admin
GRAFANA_PASSWORD=grafana_dev_2026

# API Key Secret
API_KEY_SECRET=your-api-key-encryption-secret
```

### .env.example (커밋 가능, 값 비워둠)

```properties
# PostgreSQL
DB_USERNAME=
DB_PASSWORD=

# Neo4j
NEO4J_USERNAME=
NEO4J_PASSWORD=

# Redis
REDIS_PASSWORD=

# JWT
JWT_SECRET=
JWT_ACCESS_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000

# OpenClaw
OPENCLAW_API_KEY=
OPENCLAW_BASE_URL=

# AISstream
AISSTREAM_API_KEY=

# EIA
EIA_API_KEY=

# GNews
GNEWS_API_KEY=

# Stripe
STRIPE_SECRET_KEY=
STRIPE_WEBHOOK_SECRET=

# Toss Payments
TOSS_SECRET_KEY=
TOSS_WEBHOOK_SECRET=

# n8n
N8N_BASE_URL=http://localhost:5678
N8N_USER=
N8N_PASSWORD=

# Grafana
GRAFANA_USER=
GRAFANA_PASSWORD=

# API Key Secret
API_KEY_SECRET=
```

---

## 설정 로드 순서

```
1. application.yml                 ← 공통 (항상 로드)
2. application-{profile}.yml      ← 프로필별 오버라이드
3. .env                           ← 환경변수 (spring-dotenv)
4. 시스템 환경변수                  ← 최종 오버라이드 (배포 시)
```

> 같은 키가 여러 곳에 있으면 나중에 로드된 것이 이김
> 민감 정보는 반드시 .env 또는 시스템 환경변수에서만 관리
