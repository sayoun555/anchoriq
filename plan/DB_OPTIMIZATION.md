# AnchorIQ — DB 최적화 전략

> 4개 DB (Neo4j, PostgreSQL, Elasticsearch, Redis) 최적화 설계
> 핵심 원칙: AIS 초당 수백 건 쓰기 부하를 Neo4j에서 분리하고, Redis로 실시간 처리

---

## 목차
- [DB 역할 분담](#db-역할-분담)
- [1. Neo4j 최적화](#1-neo4j-최적화)
- [2. PostgreSQL 최적화](#2-postgresql-최적화)
- [3. Elasticsearch 최적화](#3-elasticsearch-최적화)
- [4. Redis 최적화](#4-redis-최적화)
- [커넥션 풀 총괄](#커넥션-풀-총괄)

---

## DB 역할 분담

| DB | 역할 | 데이터 특성 |
|----|------|------------|
| **Neo4j** | 온톨로지 관계 그래프 | 읽기 중심, 관계 탐색 |
| **PostgreSQL** | 유저/결제/구독 | 트랜잭션, CRUD |
| **Elasticsearch** | 뉴스/AI 로그 전문 검색 | 검색, 시계열 |
| **Redis** | 실시간 캐싱 + 공간 검색 | 초고속 읽기/쓰기 |

---

## 1. Neo4j 최적화

### 1-1. 인덱스 전략

#### 문제
> 선박 수만 척의 온톨로지 그래프에서 4홉 쿼리 시 시작 노드를 빠르게 찾아야 함

#### 설계 원칙
> "자주 검색하되 자주 안 바뀌는 필드"에만 인덱스

인덱스는 읽기를 빠르게 하지만, 쓰기마다 인덱스도 갱신해야 하므로 쓰기 성능이 떨어진다.
AIS 데이터처럼 초당 수백 건 업데이트되는 필드(위치)에 인덱스를 걸면 쓰기 병목이 된다.

#### 인덱스 목록

| 노드 | 필드 | 인덱스 | 근거 |
|------|------|--------|------|
| Vessel | IMO | ✅ | 선박 조회 시작점, 불변 |
| Vessel | MMSI | ✅ | AIS 데이터 매핑 키, 불변 |
| Vessel | 선박 타입 | ✅ | 탱커/컨테이너 필터링, 불변 |
| Vessel | 국적(flag) | ✅ | 국적별 필터링, 불변 |
| Port | UN/LOCODE | ✅ | 항만 조회, 고정 데이터 |
| Company | 회사명 | ✅ | 제재 조회 시작점 |
| Country | ISO 코드 | ✅ | 제재국 필터 |
| SeaZone | 위치(lat/lon) | ✅ | 공간 검색, 고정 데이터 |
| Sanction | 제재 번호 | ✅ | 제재 목록 조회 |
| **Vessel** | **위치(lat/lon)** | **❌** | **Redis GEO로 분리** — 초당 수백 건 쓰기 |
| Chokepoint | 이름 | ❌ | 6개뿐, 풀스캔해도 무의미한 차이 |

### 1-2. 4홉 쿼리 최적화

#### 문제
> 홉이 깊어질수록 탐색 노드가 기하급수적으로 증가 (1홉 10개 → 4홉 10,000개)

#### 최적화 기법 4가지

**1) 방향 지정 — 양방향 탐색 금지**
```cypher
-- Bad: 양방향 → 노드 폭발
(v:Vessel)--(n)

-- Good: 방향 명시 → 탐색 범위 제한
(v:Vessel)-[:OWNED_BY]->(c:Company)-[:REGISTERED_IN]->(co:Country)
```

**2) 라벨 필터링 — 중간 노드 타입 명시**
```cypher
-- Bad: 아무 노드나 탐색
MATCH (v)-[*4]->(n)

-- Good: 라벨로 범위 제한
MATCH (v:Vessel)-[:OWNED_BY]->(c:Company)-[:REGISTERED_IN]->(co:Country)-[:SANCTIONED_BY]->(s:Sanction)
```

**3) 조기 필터링 — 시작점에서 WHERE**
```cypher
-- Bad: 다 찾고 나서 필터
MATCH (v:Vessel)-[*4]->(n) WHERE v.type = 'TANKER'

-- Good: 시작점부터 조건
MATCH (v:Vessel {type: 'TANKER'})-[:SAILING_ON]->(r:Route)...
```

**4) 결과 캐싱 — 반복 쿼리 Redis 저장**
```
"제재국 연관 선박 목록" → Redis에 캐싱 (TTL 1시간)
매번 4홉 안 타고 캐시에서 바로 응답
```

---

## 2. PostgreSQL 최적화

### 2-1. 인덱스

| 테이블 | 필드 | 이유 |
|--------|------|------|
| users | email | 로그인 조회 |
| subscriptions | user_id + status | 현재 구독 상태 조회 |
| payments | user_id + created_at | 결제 이력 시간순 조회 |
| api_usage | user_id + date | Free 플랜 "일 5건" 제한 체크 |
| watchlist | user_id + vessel_imo | 관심 선박 조회 |
| bookmarks | user_id + target_type + target_id | 즐겨찾기 조회 |
| notification_rules | user_id + active | 활성 알림 규칙 조회 |
| workflows | user_id + status | 워크플로우 조회 |
| workflow_executions | workflow_id + executed_at | 실행 이력 시간순 |
| audit_logs | user_id + created_at | 감사 로그 시간순 |
| api_keys | user_id + key_hash | API 키 인증 |

### 추가 테이블 스키마

**watchlist:**
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| user_id | BIGINT FK → users | |
| vessel_imo | VARCHAR(20) | |
| created_at | TIMESTAMP | |

**bookmarks:**
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| user_id | BIGINT FK → users | |
| target_type | VARCHAR(20) | PORT, ROUTE, CHOKEPOINT |
| target_id | VARCHAR(50) | LOCODE, route ID, chokepoint name |
| created_at | TIMESTAMP | |

**notification_rules:**
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| user_id | BIGINT FK → users | |
| name | VARCHAR(100) | 규칙 이름 |
| condition_type | VARCHAR(50) | RISK_LEVEL, CHOKEPOINT, VESSEL 등 |
| condition_value | VARCHAR(255) | "HIGH", "Hormuz" 등 |
| channel | VARCHAR(20) | SLACK, EMAIL |
| active | BOOLEAN | |
| created_at | TIMESTAMP | |

**workflows:**
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| user_id | BIGINT FK → users | |
| name | VARCHAR(100) | |
| n8n_workflow_id | VARCHAR(50) | n8n 내부 워크플로우 ID |
| trigger_condition | JSONB | 트리거 조건 |
| status | VARCHAR(20) | ACTIVE, INACTIVE |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

**workflow_executions:**
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| workflow_id | BIGINT FK → workflows | |
| trigger_event | JSONB | 트리거된 이벤트 |
| result | VARCHAR(20) | SUCCESS, FAILED |
| executed_at | TIMESTAMP | |

**audit_logs:**
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| user_id | BIGINT FK → users | |
| action | VARCHAR(50) | LOGIN, QUERY, EXPORT 등 |
| resource | VARCHAR(100) | 대상 리소스 |
| detail | JSONB | 상세 정보 |
| ip_address | VARCHAR(45) | |
| created_at | TIMESTAMP | |

> audit_logs도 월별 파티셔닝 적용 (created_at 기준)

**api_keys:**
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| user_id | BIGINT FK → users | |
| key_hash | VARCHAR(255) | API 키 해시 (원본 저장 X) |
| name | VARCHAR(100) | 키 이름 |
| last_used_at | TIMESTAMP | |
| created_at | TIMESTAMP | |

### 2-2. 파티셔닝

```
payments 테이블 → 월별 Range 파티셔닝

payments_2026_01
payments_2026_02
payments_2026_03
...
```

결제 이력은 쌓이기만 하고, 조회는 최근 위주.
월별 파티셔닝으로 오래된 파티션은 안 건드림 → 쿼리 속도 유지.

### 2-3. 커넥션 풀 (HikariCP)

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10
      connection-timeout: 3000    # 3초 안에 못 잡으면 에러
      idle-timeout: 600000        # 10분 놀면 반환
```

### 2-4. 쿼리 최적화

| 기법 | 적용 대상 |
|------|----------|
| **N+1 방지** | JPA `@EntityGraph` 또는 `fetch join` |
| **페이지네이션** | 결제 이력 조회 → `Pageable` |
| **읽기 전용** | 조회 쿼리 → `@Transactional(readOnly = true)` |

---

## 3. Elasticsearch 최적화

### 3-1. 설계 원칙

> "가볍게 쓴다" — 인덱스 2개, 싱글 노드, 자동 삭제

Elasticsearch는 뉴스 전문 검색 + AI 판단 로그 검색에만 사용.
하루 100건 뉴스에 ES 클러스터는 과함 → 최소 설정으로 운영.

### 3-2. 인덱스 설계

| 인덱스 | 데이터 | 보관 기간 | 삭제 |
|--------|--------|----------|------|
| `news` | GNews 뉴스 | 90일 | ILM 자동 |
| `ai-decisions` | AI 판단 로그 | 180일 | ILM 자동 |
| `anomaly-events` | 이상 탐지 이력 (AIS 소실, 항로 이탈 등) | 180일 | ILM 자동 |
| `geopolitical-events` | GDELT 지정학 이벤트 | 90일 | ILM 자동 |

### 3-3. 샤드 설정

```
샤드: 1개
레플리카: 0개
```

싱글 노드에 샤드 5개 레플리카 2개는 과최적화.
포폴 면접에서 "왜 이렇게 했나요?" → "싱글 노드라 1/0이 최적입니다" 답변 가능.

### 3-4. 한국어 형태소 분석

```
nori 플러그인 적용
"해운 파업" 검색 → "해운", "파업" 분리 → 정확한 검색 결과
```

### 3-5. 커넥션 풀

```
커넥션 풀: 20 (단순 검색 위주, 30 불필요)
```

---

## 4. Redis 최적화

### 4-1. 설계 원칙

> Redis가 이 프로젝트의 실시간 심장.
> AIS 초당 수백 건 위치 데이터 + 공간 검색 + 캐싱 전부 Redis가 처리.

### 4-2. 캐싱 대상 + 자료구조 + TTL

| 데이터 | 자료구조 | TTL | 근거 |
|--------|---------|-----|------|
| 선박 위치 | **GEO** | 30초 | AIS 갱신 주기, 공간 검색 필수 |
| 제재 선박 목록 | SET | 1시간 | 하루 몇 번 변경 |
| AI 판단 결과 | STRING | 5분 | 같은 질의 반복 방지 |
| 항만 혼잡도 | STRING | 30분 | 크롤링 주기에 맞춤 |
| API 사용량 카운터 | STRING + INCR | 자정 리셋 | Free 플랜 "일 5건" 체크 |

### 4-3. 공간 검색 (킬러 기능)

```redis
-- 선박 위치 저장
GEOADD vessels:positions 129.0 35.1 "IMO:9811000"

-- "호르무즈 해협 반경 50km 선박 조회"
GEORADIUS vessels:positions 56.0 26.5 50 km
```

Neo4j에 위치 인덱스를 안 건 이유:
- 위치는 초당 수백 건 업데이트 → Neo4j 인덱스 쓰기 병목
- Redis GEO는 인메모리 → 공간 검색 ms 단위 응답

### 4-4. Eviction 정책

```
maxmemory: 256mb
policy: allkeys-lru (가장 오래 안 쓴 키부터 삭제)
```

선박 위치는 TTL 30초라 자동 만료 → 메모리 부족 시 오래된 AI 캐시부터 삭제.

### 4-5. 장애 대비

```
Redis 죽으면?
→ 캐시 미스 → Neo4j/PostgreSQL 직접 조회
→ 느리지만 서비스는 동작
→ Spring @Cacheable fallback 설정
```

포폴에서 **"Redis 장애 시에도 서비스 정상 동작합니다"** → 인상적인 한 마디.

### 4-6. 커넥션 풀

```
커넥션 풀: 20 (단순 GET/SET, 빠르게 반환)
```

---

## 커넥션 풀 총괄

| DB | 풀 사이즈 | 근거 |
|----|----------|------|
| PostgreSQL | 30 | 메인 트랜잭션, 결제/구독 처리 |
| Neo4j | 30 | 4홉 쿼리가 커넥션 오래 잡음 |
| Elasticsearch | 20 | 단순 검색 2개 인덱스 |
| Redis | 20 | 단순 GET/SET, ms 단위 반환 |
| **총합** | **100** | Docker 환경 기준 적정 |
