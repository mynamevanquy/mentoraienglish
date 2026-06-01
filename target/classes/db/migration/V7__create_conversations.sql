-- ============================================================
-- V8: Create conversations and conversation_messages tables
-- ============================================================

-- ── conversations ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS conversations (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    user_id             UUID            NOT NULL,
    title               VARCHAR(255)    NOT NULL,
    system_prompt       TEXT,
    model_used          VARCHAR(50)     NOT NULL,
    total_tokens_used   INT             NOT NULL DEFAULT 0,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, ARCHIVED

    -- Audit columns
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMP WITH TIME ZONE,

    CONSTRAINT pk_conversations             PRIMARY KEY (id),
    CONSTRAINT fk_conversations_user        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_conversations_status      CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

COMMENT ON TABLE conversations IS 'Chat sessions between user and AI Tutor';

-- ── conversation_messages ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS conversation_messages (
    id                      UUID            NOT NULL DEFAULT gen_random_uuid(),
    conversation_id         UUID            NOT NULL,
    role                    VARCHAR(20)     NOT NULL, -- USER, ASSISTANT, SYSTEM
    content                 TEXT            NOT NULL,
    tokens_used             INT,
    grammar_corrections     JSONB,
    vocabulary_suggestions  JSONB,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT pk_conversation_messages     PRIMARY KEY (id),
    CONSTRAINT fk_messages_conversation     FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE,
    CONSTRAINT ck_messages_role             CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM'))
);

COMMENT ON TABLE conversation_messages IS 'Individual messages within a conversation history';
