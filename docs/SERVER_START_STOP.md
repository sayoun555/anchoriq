# AnchorIQ 서버 시작/종료 가이드

## 시작 (순서대로)

```bash
# 1. Docker 인프라 (PostgreSQL, Redis, Kafka, Neo4j, n8n, Elasticsearch)
cd anchoriq/infra && docker-compose --profile core --profile data up -d

# 2. Kafka healthy 될 때까지 대기 (약 30초)
docker ps | grep kafka  # (healthy) 확인

# 3. Spring Boot 백엔드
cd anchoriq/backend && ./gradlew :anchoriq-api:bootRun --args='--spring.profiles.active=local'

# 4. Next.js 프론트엔드 (별도 터미널)
cd anchoriq/frontend && pnpm dev

# 5. AIS 수집기 시작 (브라우저에서 Settings → 전체 시작, 또는 API)
curl -X POST http://localhost:8082/api/collectors/ais/start \
  -H "Cookie: access_token=<토큰>"
```

## 종료 (역순)

```bash
# 1. Spring Boot 종료
ps aux | grep "java.*anchoriq" | grep -v grep | awk '{print $2}' | xargs kill

# 2. Next.js 종료 (Ctrl+C)

# 3. Docker 인프라 종료
cd anchoriq/infra && docker-compose down

# 볼륨 포함 완전 초기화 (데이터 삭제)
cd anchoriq/infra && docker-compose down -v
```

## 포트 정리

| 서비스 | 포트 |
|--------|------|
| Spring Boot | 8082 |
| Next.js | 3004 |
| PostgreSQL | 5433 |
| Redis (Docker) | 6380 |
| Kafka | 29092 |
| Neo4j Browser | 7474 |
| Neo4j Bolt | 7687 |
| n8n | 5678 |
| Elasticsearch | 9200 |

## 접속 URL

- 프론트: http://localhost:3004
- 백엔드 API: http://localhost:8082
- Neo4j Browser: http://localhost:7474
- n8n: http://localhost:5678
- 테스트 계정: test@anchoriq.com / test1234!
