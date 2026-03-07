-- V1__Create_initial_schema.sql
-- Migração Inicial do JADE com as configurações do dia 06/02 (UTC e BigInt)

-- 1. Tabela de usuários
CREATE TABLE users
(
    id         BIGSERIAL PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    email      VARCHAR(100) NOT NULL UNIQUE,
    pswd_hash  VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    role       VARCHAR(20)  NOT NULL DEFAULT 'ROLE_USER'
);

-- 2. Tabela de monitores
CREATE TABLE monitors
(
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name             VARCHAR(100) NOT NULL,
    url              VARCHAR(255) NOT NULL,
    interval_seconds INT         DEFAULT 300,
    is_active        BOOLEAN     DEFAULT TRUE,
    last_checked     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 3. Tabela de histórico (logs de uptime)
CREATE TABLE monitor_history
(
    id            BIGSERIAL PRIMARY KEY,
    monitor_id    BIGINT NOT NULL REFERENCES monitors (id) ON DELETE CASCADE,
    status_code   INT,
    latency_ms    INT,
    is_successful BOOLEAN,
    checked_at    TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 4. Tabela de incidentes
CREATE TABLE incidents
(
    id          BIGSERIAL PRIMARY KEY,
    monitor_id  BIGINT       NOT NULL REFERENCES monitors (id) ON DELETE CASCADE,
    title       VARCHAR(150) NOT NULL,
    severity    VARCHAR(20)  NOT NULL,
    description TEXT,
    status      VARCHAR(20) DEFAULT 'OPEN',
    created_at  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    ended_at    TIMESTAMPTZ
);
