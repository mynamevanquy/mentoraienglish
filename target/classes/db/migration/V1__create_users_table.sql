-- ============================================================
-- V2: Create users table and persistent_logins table
-- ============================================================

-- ── users ──────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    full_name       VARCHAR(150) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'USER',
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,

    -- Audit columns (managed by Spring Data JPA auditing)
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMP WITH TIME ZONE,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),

    CONSTRAINT pk_users                PRIMARY KEY (id),
    CONSTRAINT uq_users_email          UNIQUE (email),
    CONSTRAINT ck_users_role           CHECK (role IN ('USER', 'ADMIN'))
);

CREATE INDEX IF NOT EXISTS idx_users_email   ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_role    ON users (role);
CREATE INDEX IF NOT EXISTS idx_users_enabled ON users (enabled) WHERE enabled = FALSE;

-- ── persistent_logins ──────────────────────────────────────────────────────
-- Required by Spring Security JdbcTokenRepositoryImpl for remember-me support.
-- Table name and column names are fixed by the Spring Security contract.
CREATE TABLE IF NOT EXISTS persistent_logins (
    username    VARCHAR(64)  NOT NULL,
    series      VARCHAR(64)  NOT NULL,
    token       VARCHAR(64)  NOT NULL,
    last_used   TIMESTAMP    NOT NULL,

    CONSTRAINT pk_persistent_logins PRIMARY KEY (series)
);
