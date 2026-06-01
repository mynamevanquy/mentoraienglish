-- ============================================================
-- V3: Create AI prompts, AI logs, and Audit logs tables
-- ============================================================

-- ── ai_prompts ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ai_prompts (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    name            VARCHAR(100)    NOT NULL,
    template        TEXT            NOT NULL,

    -- Audit columns
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT pk_ai_prompts        PRIMARY KEY (id),
    CONSTRAINT uq_ai_prompts_name   UNIQUE (name)
);

CREATE INDEX IF NOT EXISTS idx_ai_prompts_name ON ai_prompts (name);

-- ── ai_logs ──────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ai_logs (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    user_id             UUID            NOT NULL,
    prompt_tokens       INT             NOT NULL,
    completion_tokens   INT             NOT NULL,
    total_tokens        INT             NOT NULL,
    latency_ms          BIGINT          NOT NULL,
    model               VARCHAR(50)     NOT NULL,
    error_message       TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT pk_ai_logs           PRIMARY KEY (id),
    CONSTRAINT fk_ai_logs_users     FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ai_logs_user_created ON ai_logs (user_id, created_at DESC);

-- ── audit_logs ───────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS audit_logs (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    event_type      VARCHAR(100)    NOT NULL,
    details         TEXT            NOT NULL,
    ip_address      VARCHAR(50)     NOT NULL,
    user_id         UUID,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT pk_audit_logs        PRIMARY KEY (id),
    CONSTRAINT fk_audit_logs_users  FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_event ON audit_logs (event_type);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created ON audit_logs (created_at DESC);
