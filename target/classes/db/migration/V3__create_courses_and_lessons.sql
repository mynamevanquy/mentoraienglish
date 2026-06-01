-- ============================================================
-- V4: Create courses, lessons, and lesson_contents tables
-- ============================================================

-- ── courses ──────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS courses (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    title           VARCHAR(255)    NOT NULL,
    description     TEXT,
    level           VARCHAR(20)     NOT NULL DEFAULT 'BEGINNER',
    thumbnail_url   VARCHAR(500),
    is_published    BOOLEAN         NOT NULL DEFAULT FALSE,
    order_index     INT             NOT NULL DEFAULT 0,

    -- Audit columns
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMP WITH TIME ZONE,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),

    CONSTRAINT pk_courses           PRIMARY KEY (id),
    CONSTRAINT ck_courses_level     CHECK (level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED'))
);

COMMENT ON TABLE courses IS 'English learning courses organized by difficulty level';

-- ── lessons ──────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS lessons (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    course_id       UUID            NOT NULL,
    title           VARCHAR(255)    NOT NULL,
    description     TEXT,
    content_type    VARCHAR(20)     NOT NULL DEFAULT 'TEXT',
    order_index     INT             NOT NULL DEFAULT 0,
    duration_minutes INT,
    is_published    BOOLEAN         NOT NULL DEFAULT FALSE,

    -- Audit columns
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMP WITH TIME ZONE,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),

    CONSTRAINT pk_lessons           PRIMARY KEY (id),
    CONSTRAINT fk_lessons_course    FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE,
    CONSTRAINT ck_lessons_content   CHECK (content_type IN ('TEXT', 'VIDEO', 'AUDIO', 'IMAGE'))
);

COMMENT ON TABLE lessons IS 'Individual lessons belonging to a course';

-- ── lesson_contents ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS lesson_contents (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    lesson_id       UUID            NOT NULL,
    content_type    VARCHAR(20)     NOT NULL DEFAULT 'TEXT',
    content_url     VARCHAR(500),
    content_text    TEXT,
    order_index     INT             NOT NULL DEFAULT 0,
    metadata        JSONB,

    -- Audit columns
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMP WITH TIME ZONE,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),

    CONSTRAINT pk_lesson_contents           PRIMARY KEY (id),
    CONSTRAINT fk_lesson_contents_lesson    FOREIGN KEY (lesson_id) REFERENCES lessons (id) ON DELETE CASCADE,
    CONSTRAINT ck_lesson_contents_type      CHECK (content_type IN ('TEXT', 'VIDEO', 'AUDIO', 'IMAGE'))
);

COMMENT ON TABLE lesson_contents IS 'Rich content blocks within a lesson (text, media, etc.)';
