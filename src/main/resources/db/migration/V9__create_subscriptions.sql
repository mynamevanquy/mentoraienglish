-- ============================================================
-- V10: Create subscriptions table
-- ============================================================

-- ── subscriptions ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS subscriptions (
    id                      UUID            NOT NULL DEFAULT gen_random_uuid(),
    user_id                 UUID            NOT NULL,
    plan                    VARCHAR(20)     NOT NULL DEFAULT 'FREE', -- FREE, PRO, PREMIUM
    status                  VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, CANCELLED, EXPIRED
    started_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at              TIMESTAMP WITH TIME ZONE,
    stripe_subscription_id  VARCHAR(255),
    metadata                JSONB,

    CONSTRAINT pk_subscriptions             PRIMARY KEY (id),
    CONSTRAINT fk_subscriptions_user        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_subscriptions_plan        CHECK (plan IN ('FREE', 'PRO', 'PREMIUM')),
    CONSTRAINT ck_subscriptions_status      CHECK (status IN ('ACTIVE', 'CANCELLED', 'EXPIRED'))
);

COMMENT ON TABLE subscriptions IS 'User subscription statuses and plans';
