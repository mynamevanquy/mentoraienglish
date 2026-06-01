-- ============================================================
-- V9: Create study_sessions and learning_progress tables
-- ============================================================

-- ── study_sessions ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS study_sessions (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    user_id             UUID            NOT NULL,
    started_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    ended_at            TIMESTAMP WITH TIME ZONE,
    duration_minutes    INT,
    activity_type       VARCHAR(30)     NOT NULL, -- LESSON, EXERCISE, VOCABULARY, etc.
    activity_id         UUID,
    xp_earned           INT             NOT NULL DEFAULT 0,

    CONSTRAINT pk_study_sessions            PRIMARY KEY (id),
    CONSTRAINT fk_study_sessions_user       FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_study_sessions_activity   CHECK (activity_type IN ('LESSON', 'EXERCISE', 'VOCABULARY', 'CONVERSATION', 'GRAMMAR'))
);

COMMENT ON TABLE study_sessions IS 'Detailed user study logs and XP tracking';

-- ── learning_progress ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS learning_progress (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    user_id             UUID            NOT NULL,
    metric_type         VARCHAR(50)     NOT NULL, -- VOCABULARY_MASTERED, EXERCISES_COMPLETED, etc.
    metric_value        DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    recorded_date       DATE            NOT NULL DEFAULT CURRENT_DATE,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT pk_learning_progress         PRIMARY KEY (id),
    CONSTRAINT fk_learning_progress_user    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_user_metric_date          UNIQUE (user_id, metric_type, recorded_date),
    CONSTRAINT ck_learning_progress_metric  CHECK (metric_type IN ('VOCABULARY_MASTERED', 'EXERCISES_COMPLETED', 'STREAK_DAYS', 'XP_TOTAL', 'LESSONS_COMPLETED'))
);

COMMENT ON TABLE learning_progress IS 'Daily aggregated stats per user for dashboard graphs';
