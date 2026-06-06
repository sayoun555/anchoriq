# AnchorIQ — DB 초기화 스크립트

> PostgreSQL 테이블 생성 + 파티셔닝 + Neo4j 인덱스/제약 조건
> Docker 첫 기동 시 자동 실행되도록 설계

---

## 목차
- [PostgreSQL 초기화](#postgresql-초기화)
- [Neo4j 초기화](#neo4j-초기화)
- [Elasticsearch 초기화](#elasticsearch-초기화)
- [Redis 초기 데이터](#redis-초기-데이터)

---

## PostgreSQL 초기화

> `/infra/sql/init.sql` → Docker `docker-entrypoint-initdb.d/`에 마운트

```sql
-- ============================================
-- AnchorIQ PostgreSQL 초기화 스크립트
-- ============================================

-- 확장 모듈
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- account 도메인
-- ============================================

-- users
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    name            VARCHAR(100) NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);

-- subscriptions
CREATE TABLE subscriptions (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users(id),
    plan            VARCHAR(20)  NOT NULL DEFAULT 'FREE',
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    started_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMP,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_subscriptions_user_status ON subscriptions(user_id, status);

-- payments (월별 파티셔닝)
CREATE TABLE payments (
    id                      BIGSERIAL,
    user_id                 BIGINT       NOT NULL,
    gateway                 VARCHAR(20)  NOT NULL,
    gateway_payment_id      VARCHAR(255),
    amount                  DECIMAL(10,2) NOT NULL,
    currency                VARCHAR(3)   NOT NULL,
    status                  VARCHAR(20)  NOT NULL,
    created_at              TIMESTAMP    NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- 2026년 월별 파티션 생성
CREATE TABLE payments_2026_01 PARTITION OF payments
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
CREATE TABLE payments_2026_02 PARTITION OF payments
    FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');
CREATE TABLE payments_2026_03 PARTITION OF payments
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');
CREATE TABLE payments_2026_04 PARTITION OF payments
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');
CREATE TABLE payments_2026_05 PARTITION OF payments
    FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');
CREATE TABLE payments_2026_06 PARTITION OF payments
    FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');
CREATE TABLE payments_2026_07 PARTITION OF payments
    FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');
CREATE TABLE payments_2026_08 PARTITION OF payments
    FOR VALUES FROM ('2026-08-01') TO ('2026-09-01');
CREATE TABLE payments_2026_09 PARTITION OF payments
    FOR VALUES FROM ('2026-09-01') TO ('2026-10-01');
CREATE TABLE payments_2026_10 PARTITION OF payments
    FOR VALUES FROM ('2026-10-01') TO ('2026-11-01');
CREATE TABLE payments_2026_11 PARTITION OF payments
    FOR VALUES FROM ('2026-11-01') TO ('2026-12-01');
CREATE TABLE payments_2026_12 PARTITION OF payments
    FOR VALUES FROM ('2026-12-01') TO ('2027-01-01');

CREATE INDEX idx_payments_user_created ON payments(user_id, created_at);

-- api_usage
CREATE TABLE api_usage (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users(id),
    date            DATE         NOT NULL,
    count           INTEGER      NOT NULL DEFAULT 0,
    UNIQUE (user_id, date)
);

CREATE INDEX idx_api_usage_user_date ON api_usage(user_id, date);

-- api_keys
CREATE TABLE api_keys (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users(id),
    key_hash        VARCHAR(255) NOT NULL,
    name            VARCHAR(100),
    last_used_at    TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_api_keys_user ON api_keys(user_id);
CREATE INDEX idx_api_keys_hash ON api_keys(key_hash);

-- ============================================
-- operation 도메인
-- ============================================

-- watchlist
CREATE TABLE watchlist (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users(id),
    vessel_imo      VARCHAR(20)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, vessel_imo)
);

CREATE INDEX idx_watchlist_user ON watchlist(user_id, vessel_imo);

-- bookmarks
CREATE TABLE bookmarks (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users(id),
    target_type     VARCHAR(20)  NOT NULL,
    target_id       VARCHAR(50)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, target_type, target_id)
);

CREATE INDEX idx_bookmarks_user ON bookmarks(user_id, target_type, target_id);

-- notification_rules
CREATE TABLE notification_rules (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users(id),
    name            VARCHAR(100) NOT NULL,
    condition_type  VARCHAR(50)  NOT NULL,
    condition_value VARCHAR(255) NOT NULL,
    channel         VARCHAR(20)  NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notification_rules_user ON notification_rules(user_id, active);

-- notification_settings (유저별 채널 발송 대상 — 규칙=조건/채널, 설정=목적지)
CREATE TABLE notification_settings (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT       NOT NULL UNIQUE REFERENCES users(id),
    slack_enabled     BOOLEAN      NOT NULL DEFAULT FALSE,
    slack_webhook_url VARCHAR(500),
    email_enabled     BOOLEAN      NOT NULL DEFAULT FALSE,
    email_address     VARCHAR(255),
    version           BIGINT       NOT NULL DEFAULT 0,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- workflows
CREATE TABLE workflows (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT       NOT NULL REFERENCES users(id),
    name                VARCHAR(100) NOT NULL,
    n8n_workflow_id     VARCHAR(50),
    trigger_condition   JSONB,
    status              VARCHAR(20)  NOT NULL DEFAULT 'INACTIVE',
    version             BIGINT       NOT NULL DEFAULT 0,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_workflows_user ON workflows(user_id, status);

-- workflow_executions
CREATE TABLE workflow_executions (
    id              BIGSERIAL PRIMARY KEY,
    workflow_id     BIGINT       NOT NULL REFERENCES workflows(id),
    trigger_event   JSONB,
    result          VARCHAR(20)  NOT NULL,
    executed_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_workflow_executions_workflow ON workflow_executions(workflow_id, executed_at);

-- audit_logs (월별 파티셔닝)
CREATE TABLE audit_logs (
    id              BIGSERIAL,
    user_id         BIGINT       NOT NULL,
    action          VARCHAR(50)  NOT NULL,
    resource        VARCHAR(100),
    detail          JSONB,
    ip_address      VARCHAR(45),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- 2026년 월별 파티션
CREATE TABLE audit_logs_2026_01 PARTITION OF audit_logs
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
CREATE TABLE audit_logs_2026_02 PARTITION OF audit_logs
    FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');
CREATE TABLE audit_logs_2026_03 PARTITION OF audit_logs
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');
CREATE TABLE audit_logs_2026_04 PARTITION OF audit_logs
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');
CREATE TABLE audit_logs_2026_05 PARTITION OF audit_logs
    FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');
CREATE TABLE audit_logs_2026_06 PARTITION OF audit_logs
    FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');
CREATE TABLE audit_logs_2026_07 PARTITION OF audit_logs
    FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');
CREATE TABLE audit_logs_2026_08 PARTITION OF audit_logs
    FOR VALUES FROM ('2026-08-01') TO ('2026-09-01');
CREATE TABLE audit_logs_2026_09 PARTITION OF audit_logs
    FOR VALUES FROM ('2026-09-01') TO ('2026-10-01');
CREATE TABLE audit_logs_2026_10 PARTITION OF audit_logs
    FOR VALUES FROM ('2026-10-01') TO ('2026-11-01');
CREATE TABLE audit_logs_2026_11 PARTITION OF audit_logs
    FOR VALUES FROM ('2026-11-01') TO ('2026-12-01');
CREATE TABLE audit_logs_2026_12 PARTITION OF audit_logs
    FOR VALUES FROM ('2026-12-01') TO ('2027-01-01');

CREATE INDEX idx_audit_logs_user ON audit_logs(user_id, created_at);

-- ============================================
-- 초기 데이터
-- ============================================

-- 관리자 계정 (비밀번호: admin123! → BCrypt 해시)
INSERT INTO users (email, password, name, role)
VALUES ('admin@anchoriq.com', '$2a$10$placeholder_bcrypt_hash', 'Admin', 'ADMIN');

INSERT INTO subscriptions (user_id, plan, status)
VALUES (1, 'ENTERPRISE', 'ACTIVE');
```

---

## Neo4j 초기화

> Spring Boot 앱 시작 시 실행하거나, Neo4j Browser에서 직접 실행

### 인덱스 생성

```cypher
// ============================================
// AnchorIQ Neo4j 인덱스 + 제약 조건
// ============================================

// Vessel 인덱스
CREATE INDEX vessel_imo IF NOT EXISTS FOR (v:Vessel) ON (v.imo);
CREATE INDEX vessel_mmsi IF NOT EXISTS FOR (v:Vessel) ON (v.mmsi);
CREATE INDEX vessel_type IF NOT EXISTS FOR (v:Vessel) ON (v.type);
CREATE INDEX vessel_flag IF NOT EXISTS FOR (v:Vessel) ON (v.flag);

// Vessel 유니크 제약
CREATE CONSTRAINT vessel_imo_unique IF NOT EXISTS FOR (v:Vessel) REQUIRE v.imo IS UNIQUE;
CREATE CONSTRAINT vessel_mmsi_unique IF NOT EXISTS FOR (v:Vessel) REQUIRE v.mmsi IS UNIQUE;

// Port 인덱스
CREATE INDEX port_locode IF NOT EXISTS FOR (p:Port) ON (p.locode);
CREATE CONSTRAINT port_locode_unique IF NOT EXISTS FOR (p:Port) REQUIRE p.locode IS UNIQUE;

// Company 인덱스
CREATE INDEX company_name IF NOT EXISTS FOR (c:Company) ON (c.name);

// Country 인덱스
CREATE INDEX country_code IF NOT EXISTS FOR (c:Country) ON (c.code);
CREATE CONSTRAINT country_code_unique IF NOT EXISTS FOR (c:Country) REQUIRE c.code IS UNIQUE;

// Sanction 인덱스
CREATE INDEX sanction_ref IF NOT EXISTS FOR (s:Sanction) ON (s.referenceNumber);

// SeaZone 공간 인덱스
CREATE POINT INDEX seazone_location IF NOT EXISTS FOR (sz:SeaZone) ON (sz.location);
```

### 초기 데이터 — 초크포인트 6개

```cypher
// ============================================
// 초크포인트 (고정 데이터)
// ============================================

CREATE (c:Chokepoint {
  name: 'Hormuz',
  displayName: '호르무즈 해협',
  lat: 26.5667,
  lon: 56.2500,
  riskLevel: 'HIGH',
  description: '이란-오만 사이, 세계 원유 수송의 핵심'
});

CREATE (c:Chokepoint {
  name: 'Malacca',
  displayName: '말라카 해협',
  lat: 2.5000,
  lon: 101.2000,
  riskLevel: 'MEDIUM',
  description: '말레이시아-인도네시아 사이, 아시아-유럽 최단 경로'
});

CREATE (c:Chokepoint {
  name: 'Suez',
  displayName: '수에즈 운하',
  lat: 30.4500,
  lon: 32.3500,
  riskLevel: 'LOW',
  description: '이집트, 아시아-유럽 항로 핵심'
});

CREATE (c:Chokepoint {
  name: 'Bab el-Mandeb',
  displayName: '바브엘만데브 해협',
  lat: 12.5833,
  lon: 43.3333,
  riskLevel: 'HIGH',
  description: '예멘-지부티 사이, 수에즈 진입로'
});

CREATE (c:Chokepoint {
  name: 'Taiwan Strait',
  displayName: '대만 해협',
  lat: 24.0000,
  lon: 119.0000,
  riskLevel: 'MEDIUM',
  description: '중국-대만 사이, 반도체 공급망 핵심'
});

CREATE (c:Chokepoint {
  name: 'Panama',
  displayName: '파나마 운하',
  lat: 9.1000,
  lon: -79.6800,
  riskLevel: 'LOW',
  description: '태평양-대서양 연결, 미국 동부행 핵심'
});
```

### 초기 데이터 — 주요 항로

```cypher
// ============================================
// 주요 항로 (한국 기준 핵심 3개)
// ============================================

CREATE (r:Route {
  name: 'Asia-MidEast',
  displayName: '아시아-중동 (원유)',
  distance: 6500,
  unit: 'nm'
});

CREATE (r:Route {
  name: 'Asia-Europe',
  displayName: '아시아-유럽 (수출)',
  distance: 11000,
  unit: 'nm'
});

CREATE (r:Route {
  name: 'Asia-Americas',
  displayName: '아시아-미국 (수출)',
  distance: 5500,
  unit: 'nm'
});

// 항로-초크포인트 관계
MATCH (r:Route {name: 'Asia-MidEast'}), (cp:Chokepoint {name: 'Malacca'})
CREATE (r)-[:PASSES_THROUGH {order: 1}]->(cp);

MATCH (r:Route {name: 'Asia-MidEast'}), (cp:Chokepoint {name: 'Hormuz'})
CREATE (r)-[:PASSES_THROUGH {order: 2}]->(cp);

MATCH (r:Route {name: 'Asia-Europe'}), (cp:Chokepoint {name: 'Malacca'})
CREATE (r)-[:PASSES_THROUGH {order: 1}]->(cp);

MATCH (r:Route {name: 'Asia-Europe'}), (cp:Chokepoint {name: 'Bab el-Mandeb'})
CREATE (r)-[:PASSES_THROUGH {order: 2}]->(cp);

MATCH (r:Route {name: 'Asia-Europe'}), (cp:Chokepoint {name: 'Suez'})
CREATE (r)-[:PASSES_THROUGH {order: 3}]->(cp);

MATCH (r:Route {name: 'Asia-Americas'}), (cp:Chokepoint {name: 'Panama'})
CREATE (r)-[:PASSES_THROUGH {order: 1}]->(cp);
```

---

## Elasticsearch 초기화

> Spring Boot 앱 시작 시 자동 생성 or curl로 수동 생성

### news 인덱스

```json
PUT /news
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0,
    "analysis": {
      "analyzer": {
        "korean": {
          "type": "custom",
          "tokenizer": "nori_tokenizer",
          "filter": ["lowercase", "nori_part_of_speech"]
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "title":       { "type": "text", "analyzer": "korean" },
      "source":      { "type": "keyword" },
      "url":         { "type": "keyword" },
      "keywords":    { "type": "keyword" },
      "publishedAt": { "type": "date" },
      "createdAt":   { "type": "date" }
    }
  }
}
```

### ai-decisions 인덱스

```json
PUT /ai-decisions
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0
  },
  "mappings": {
    "properties": {
      "type":              { "type": "keyword" },
      "riskLevel":         { "type": "keyword" },
      "vesselImo":         { "type": "keyword" },
      "vesselName":        { "type": "text" },
      "chokepoint":        { "type": "keyword" },
      "reason":            { "type": "text", "analyzer": "korean" },
      "recommendedAction": { "type": "text" },
      "aiConfidence":      { "type": "float" },
      "createdAt":         { "type": "date" }
    }
  }
}
```

### anomaly-events 인덱스

```json
PUT /anomaly-events
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0
  },
  "mappings": {
    "properties": {
      "anomalyType":  { "type": "keyword" },
      "vesselImo":    { "type": "keyword" },
      "vesselName":   { "type": "text" },
      "lastPosition": { "type": "geo_point" },
      "description":  { "type": "text" },
      "detectedAt":   { "type": "date" }
    }
  }
}
```

### geopolitical-events 인덱스

```json
PUT /geopolitical-events
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0
  },
  "mappings": {
    "properties": {
      "eventType":    { "type": "keyword" },
      "region":       { "type": "keyword" },
      "location":     { "type": "geo_point" },
      "severity":     { "type": "keyword" },
      "description":  { "type": "text", "analyzer": "korean" },
      "source":       { "type": "keyword" },
      "timestamp":    { "type": "date" }
    }
  }
}
```

### ILM (Index Lifecycle Management) 정책

```json
PUT _ilm/policy/anchoriq-retention
{
  "policy": {
    "phases": {
      "hot": {
        "actions": {}
      },
      "delete": {
        "min_age": "90d",
        "actions": {
          "delete": {}
        }
      }
    }
  }
}

PUT _ilm/policy/anchoriq-retention-long
{
  "policy": {
    "phases": {
      "hot": {
        "actions": {}
      },
      "delete": {
        "min_age": "180d",
        "actions": {
          "delete": {}
        }
      }
    }
  }
}
```

> news, geopolitical-events → `anchoriq-retention` (90일)
> ai-decisions, anomaly-events → `anchoriq-retention-long` (180일)

---

## Redis 초기 데이터

> Redis는 앱이 실행되면서 자동으로 채움 — 별도 초기화 불필요
> 아래는 데이터 구조 확인용 참고

```redis
# 선박 위치 (GEO)
GEOADD vessels:positions 129.0 35.1 "IMO:9811000"
GEOADD vessels:positions 56.0 26.5 "IMO:9722000"

# 제재 선박 목록 (SET)
SADD sanctioned:vessels "IMO:9999001" "IMO:9999002"

# API 사용량 카운터 (STRING)
SET api:usage:1:2026-03-27 "3" EX 86400

# 항만 혼잡도 캐시 (STRING)
SET port:congestion:KRPUS "85.0" EX 1800

# AI 결과 캐시 (STRING)
SET ai:result:abc123hash "{...}" EX 300
```
