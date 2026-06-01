-- ============================================================
-- V5: Create vocabularies and user_vocabularies tables
-- ============================================================

-- ── vocabularies ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS vocabularies (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    word                VARCHAR(100)    NOT NULL,
    phonetic            VARCHAR(100),
    part_of_speech      VARCHAR(50),
    definition_en       TEXT            NOT NULL,
    definition_vi       TEXT,
    example_sentences   JSONB,
    audio_url           VARCHAR(500),
    image_url           VARCHAR(500),
    difficulty_level    VARCHAR(20)     NOT NULL DEFAULT 'BEGINNER',
    tags                JSONB,

    -- Audit columns
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMP WITH TIME ZONE,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),

    CONSTRAINT pk_vocabularies                  PRIMARY KEY (id),
    CONSTRAINT ck_vocabularies_difficulty       CHECK (difficulty_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED'))
);

COMMENT ON TABLE vocabularies IS 'Central dictionary of words and phrases';

-- ── user_vocabularies ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS user_vocabularies (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    user_id             UUID            NOT NULL,
    vocabulary_id       UUID            NOT NULL,
    mastery_level       INT             NOT NULL DEFAULT 0,
    review_count        INT             NOT NULL DEFAULT 0,
    next_review_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    last_reviewed_at    TIMESTAMP WITH TIME ZONE,
    sm2_easiness_factor DECIMAL(5,2)    NOT NULL DEFAULT 2.50,
    sm2_interval        INT             NOT NULL DEFAULT 0,
    sm2_repetitions     INT             NOT NULL DEFAULT 0,
    is_bookmarked       BOOLEAN         NOT NULL DEFAULT FALSE,

    -- Audit columns
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT pk_user_vocabularies             PRIMARY KEY (id),
    CONSTRAINT fk_user_vocabularies_user        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_vocabularies_vocab       FOREIGN KEY (vocabulary_id) REFERENCES vocabularies (id) ON DELETE CASCADE,
    CONSTRAINT uq_user_vocabularies_user_word   UNIQUE (user_id, vocabulary_id)
);

COMMENT ON TABLE user_vocabularies IS 'User tracking of words with spaced repetition details';
