-- ============================================================
-- V6: Create grammar_topics table
-- ============================================================

-- ── grammar_topics ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS grammar_topics (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    title               VARCHAR(255)    NOT NULL,
    description_vi      TEXT,
    description_en      TEXT,
    level               VARCHAR(20)     NOT NULL DEFAULT 'BEGINNER',
    examples            JSONB,
    rules               JSONB,
    order_index         INT             NOT NULL DEFAULT 0,
    is_published        BOOLEAN         NOT NULL DEFAULT FALSE,

    -- Audit columns
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMP WITH TIME ZONE,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),

    CONSTRAINT pk_grammar_topics            PRIMARY KEY (id),
    CONSTRAINT ck_grammar_topics_level      CHECK (level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED'))
);

COMMENT ON TABLE grammar_topics IS 'Grammar topics and rules explanation';
