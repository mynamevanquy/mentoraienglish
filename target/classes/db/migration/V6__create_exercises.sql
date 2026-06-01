-- ============================================================
-- V7: Create exercises, questions, attempts, and answers tables
-- ============================================================

-- ── exercises ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS exercises (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    title               VARCHAR(255)    NOT NULL,
    description         TEXT,
    exercise_type       VARCHAR(30)     NOT NULL, -- MULTIPLE_CHOICE, FILL_BLANK, etc.
    source              VARCHAR(20)     NOT NULL DEFAULT 'MANUAL', -- MANUAL, AI_GENERATED
    related_lesson_id   UUID,
    related_grammar_id  UUID,
    difficulty_level    VARCHAR(20)     NOT NULL DEFAULT 'BEGINNER',
    time_limit_seconds  INT,
    is_published        BOOLEAN         NOT NULL DEFAULT FALSE,
    metadata            JSONB,

    -- Audit columns
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMP WITH TIME ZONE,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),

    CONSTRAINT pk_exercises                 PRIMARY KEY (id),
    CONSTRAINT fk_exercises_lesson          FOREIGN KEY (related_lesson_id) REFERENCES lessons (id) ON DELETE SET NULL,
    CONSTRAINT fk_exercises_grammar         FOREIGN KEY (related_grammar_id) REFERENCES grammar_topics (id) ON DELETE SET NULL,
    CONSTRAINT ck_exercises_difficulty      CHECK (difficulty_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    CONSTRAINT ck_exercises_type            CHECK (exercise_type IN ('MULTIPLE_CHOICE', 'FILL_BLANK', 'SENTENCE_REORDER', 'WRITING', 'LISTENING')),
    CONSTRAINT ck_exercises_source          CHECK (source IN ('MANUAL', 'AI_GENERATED'))
);

COMMENT ON TABLE exercises IS 'Quizzes and exercises associated with lessons or grammar topics';

-- ── exercise_questions ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS exercise_questions (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    exercise_id         UUID            NOT NULL,
    question_text       TEXT            NOT NULL,
    question_type       VARCHAR(30)     NOT NULL, -- MULTIPLE_CHOICE, FILL_BLANK, etc.
    options             JSONB,
    correct_answer      TEXT            NOT NULL,
    explanation         TEXT,
    order_index         INT             NOT NULL DEFAULT 0,
    points              INT             NOT NULL DEFAULT 1,

    CONSTRAINT pk_exercise_questions        PRIMARY KEY (id),
    CONSTRAINT fk_questions_exercise        FOREIGN KEY (exercise_id) REFERENCES exercises (id) ON DELETE CASCADE
);

COMMENT ON TABLE exercise_questions IS 'Individual questions in an exercise';

-- ── exercise_attempts ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS exercise_attempts (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    user_id             UUID            NOT NULL,
    exercise_id         UUID            NOT NULL,
    started_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    completed_at        TIMESTAMP WITH TIME ZONE,
    score               DECIMAL(5,2),
    max_score           DECIMAL(5,2),
    time_spent_seconds  INT,
    status              VARCHAR(20)     NOT NULL DEFAULT 'IN_PROGRESS', -- IN_PROGRESS, COMPLETED, ABANDONED

    CONSTRAINT pk_exercise_attempts         PRIMARY KEY (id),
    CONSTRAINT fk_attempts_user             FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_attempts_exercise         FOREIGN KEY (exercise_id) REFERENCES exercises (id) ON DELETE CASCADE,
    CONSTRAINT ck_attempts_status           CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'ABANDONED'))
);

COMMENT ON TABLE exercise_attempts IS 'User attempts on an exercise';

-- ── exercise_answers ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS exercise_answers (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    attempt_id          UUID            NOT NULL,
    question_id         UUID            NOT NULL,
    user_answer         TEXT,
    is_correct          BOOLEAN         NOT NULL DEFAULT FALSE,
    points_earned       INT             NOT NULL DEFAULT 0,
    ai_feedback         TEXT,
    answered_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT pk_exercise_answers          PRIMARY KEY (id),
    CONSTRAINT fk_answers_attempt           FOREIGN KEY (attempt_id) REFERENCES exercise_attempts (id) ON DELETE CASCADE,
    CONSTRAINT fk_answers_question          FOREIGN KEY (question_id) REFERENCES exercise_questions (id) ON DELETE CASCADE
);

COMMENT ON TABLE exercise_answers IS 'Individual responses to questions inside an attempt';
