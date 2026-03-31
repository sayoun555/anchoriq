-- ============================================
-- AnchorIQ PostgreSQL Initialization Script
-- ============================================

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- account domain
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

-- payments (monthly partitioning)
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

-- 2026 monthly partitions
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
-- operation domain
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

-- notification_history
CREATE TABLE notification_history (
    id              BIGSERIAL PRIMARY KEY,
    rule_id         BIGINT       REFERENCES notification_rules(id),
    channel         VARCHAR(20)  NOT NULL,
    destination     VARCHAR(255) NOT NULL,
    message         TEXT         NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    sent_at         TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notification_history_rule ON notification_history(rule_id, sent_at);

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

-- audit_logs (monthly partitioning)
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

-- 2026 monthly partitions
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
-- Initial Data
-- ============================================

-- Admin account (password: admin123! -> BCrypt hash)
INSERT INTO users (email, password, name, role)
VALUES ('admin@anchoriq.com', '$2a$10$placeholder_bcrypt_hash', 'Admin', 'ADMIN');

INSERT INTO subscriptions (user_id, plan, status)
VALUES (1, 'ENTERPRISE', 'ACTIVE');
