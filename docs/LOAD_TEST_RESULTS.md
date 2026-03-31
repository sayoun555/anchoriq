# k6 부하 테스트 결과

> Docker Compose 기동 후 실제 테스트를 수행하여 아래 테이블을 채운다.
> 현재는 스크립트와 예상 목표 수치를 문서화한 상태이다.

---

## 테스트 환경

- **인프라**: Docker Compose (full 프로필)
- **애플리케이션**: Spring Boot 3.4, Java 21
- **DB**: PostgreSQL 16, Neo4j 5, Redis 7, Elasticsearch 8.13
- **메시징**: Kafka (KRaft)
- **부하 도구**: k6

---

## 테스트 스크립트 목록

| 스크립트 | 설명 | 실행 명령 |
|---------|------|----------|
| `k6/ais-load-test.js` | AIS 위치 데이터 대량 POST | `k6 run backend/k6/ais-load-test.js` |
| `k6/api-load-test.js` | API 동시 요청 (4개 시나리오) | `k6 run backend/k6/api-load-test.js` |
| `k6/neo4j-load-test.js` | Neo4j 4홉 쿼리 동시 요청 | `k6 run backend/k6/neo4j-load-test.js` |
| `k6/redis-geo-load-test.js` | Redis GEO 공간 검색 | `k6 run backend/k6/redis-geo-load-test.js` |

---

## 부하 테스트 결과 (목표 수치)

### 1. AIS 데이터 유입 (ais-load-test.js)

| 지표 | 목표 | 실측 |
|------|------|------|
| VUs | 200 | - |
| 평균 응답 | < 50ms | - |
| p95 | < 200ms | - |
| 처리량 | > 5,000/sec | - |
| 에러율 | < 1% | - |

### 2. API 동시 요청 (api-load-test.js)

| 시나리오 | VUs | 평균 응답 목표 | p95 목표 | 처리량 목표 | 에러율 목표 | 실측 |
|---------|-----|-------------|---------|-----------|-----------|------|
| 선박 목록 조회 | 80 | < 100ms | < 300ms | > 200/sec | < 1% | - |
| 리스크 스코어 | 50 | < 200ms | < 500ms | > 100/sec | < 1% | - |
| AI 질의 | 30 | < 1500ms | < 3000ms | > 10/sec | < 1% | - |
| 글로벌 검색 | 40 | < 150ms | < 400ms | > 150/sec | < 1% | - |

### 3. Neo4j 4홉 쿼리 (neo4j-load-test.js)

| 시나리오 | VUs | 평균 응답 목표 | p95 목표 | 처리량 목표 | 에러율 목표 | 실측 |
|---------|-----|-------------|---------|-----------|-----------|------|
| 그래프 확장 (1~4홉) | 50 | < 500ms | < 1500ms | > 20/sec | < 2% | - |
| 최단 경로 | 30 | < 800ms | < 2000ms | > 10/sec | < 2% | - |
| 제재 네트워크 | 20 | < 800ms | < 2000ms | > 5/sec | < 2% | - |

### 4. Redis GEO 검색 (redis-geo-load-test.js)

| 지표 | 목표 | 실측 |
|------|------|------|
| VUs | 100 | - |
| 평균 응답 | < 20ms | - |
| p95 | < 100ms | - |
| 처리량 | > 1,000/sec | - |
| 에러율 | < 1% | - |

---

## 병목 분석 및 최적화 기록

> 테스트 후 발견한 병목과 최적화 내역을 기록한다.

| 병목 지점 | 원인 | 해결 방법 | Before | After |
|----------|------|----------|--------|-------|
| - | - | - | - | - |

---

## 실행 방법

```bash
# 1. 인프라 기동
cd anchoriq/infra && docker compose --profile full up -d

# 2. 애플리케이션 기동 확인
curl http://localhost:8080/actuator/health

# 3. 부하 테스트 실행
k6 run backend/k6/ais-load-test.js
k6 run backend/k6/api-load-test.js
k6 run backend/k6/neo4j-load-test.js
k6 run backend/k6/redis-geo-load-test.js

# 환경변수로 대상 서버 변경 가능
k6 run -e BASE_URL=http://your-server:8080 backend/k6/api-load-test.js

# 인증 토큰 전달
k6 run -e AUTH_TOKEN=your-jwt-token backend/k6/api-load-test.js
```
