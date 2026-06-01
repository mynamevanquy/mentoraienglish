You are a senior PostgreSQL database engineer.

Context:
- Project: AI English Learning Platform
- DB: PostgreSQL 15
- Migration tool: Flyway
- All PKs: UUID
- Soft delete: deleted_at TIMESTAMP NULL
- Audit fields: created_at, updated_at, created_by, updated_by

## Task
Generate Flyway SQL migration files. Each file is numbered sequentially.

## Files to generate

### V1__create_users_and_roles.sql
Tables:
- users (id, email, password_hash, full_name, avatar_url, is_active, last_login_at, deleted_at, audit fields)
- roles (id, name ENUM: USER/ADMIN, description)
- user_roles (user_id FK, role_id FK, assigned_at)

### V2__create_courses_and_lessons.sql
Tables:
- courses (id, title, description, level ENUM: BEGINNER/INTERMEDIATE/ADVANCED, thumbnail_url, is_published, order_index, deleted_at, audit fields)
- lessons (id, course_id FK, title, description, content_type ENUM, order_index, duration_minutes, is_published, deleted_at, audit fields)
- lesson_contents (id, lesson_id FK, content_type ENUM: TEXT/VIDEO/AUDIO/IMAGE, content_url, content_text TEXT, order_index, metadata JSONB)

### V3__create_vocabulary.sql
Tables:
- vocabularies (id, word, phonetic, part_of_speech, definition_en TEXT, definition_vi TEXT, example_sentences JSONB, audio_url, image_url, difficulty_level, tags JSONB, deleted_at, audit fields)
- user_vocabularies (id, user_id FK, vocabulary_id FK, mastery_level INT DEFAULT 0, review_count INT DEFAULT 0, next_review_at TIMESTAMP, last_reviewed_at, sm2_easiness_factor DECIMAL, sm2_interval INT, sm2_repetitions INT, is_bookmarked BOOL, audit fields)

### V4__create_grammar.sql
Tables:
- grammar_topics (id, title, description_vi TEXT, description_en TEXT, level ENUM, examples JSONB, rules JSONB, order_index, is_published, deleted_at, audit fields)

### V5__create_exercises.sql
Tables:
- exercises (id, title, description, exercise_type ENUM: MULTIPLE_CHOICE/FILL_BLANK/SENTENCE_REORDER/WRITING/LISTENING, source ENUM: MANUAL/AI_GENERATED, related_lesson_id FK NULL, related_grammar_id FK NULL, difficulty_level, time_limit_seconds INT, is_published, metadata JSONB, deleted_at, audit fields)
- exercise_questions (id, exercise_id FK, question_text TEXT, question_type ENUM, options JSONB, correct_answer TEXT, explanation TEXT, order_index, points INT DEFAULT 1)
- exercise_attempts (id, user_id FK, exercise_id FK, started_at, completed_at, score DECIMAL, max_score DECIMAL, time_spent_seconds INT, status ENUM: IN_PROGRESS/COMPLETED/ABANDONED)
- exercise_answers (id, attempt_id FK, question_id FK, user_answer TEXT, is_correct BOOL, points_earned INT, ai_feedback TEXT, answered_at)

### V6__create_conversations.sql
Tables:
- conversations (id, user_id FK, title, system_prompt TEXT, model_used VARCHAR, total_tokens_used INT DEFAULT 0, status ENUM: ACTIVE/ARCHIVED, created_at, updated_at, deleted_at)
- conversation_messages (id, conversation_id FK, role ENUM: USER/ASSISTANT/SYSTEM, content TEXT, tokens_used INT, grammar_corrections JSONB NULL, vocabulary_suggestions JSONB NULL, created_at)

### V7__create_ai_infra.sql
Tables:
- ai_prompts (id, name UNIQUE, prompt_template TEXT, variables JSONB, version INT, is_active BOOL, created_at, updated_at)
- ai_logs (id, user_id FK NULL, prompt_name VARCHAR, model VARCHAR, input_tokens INT, output_tokens INT, latency_ms INT, status ENUM: SUCCESS/ERROR, error_message TEXT, created_at)

### V8__create_progress_and_sessions.sql
Tables:
- study_sessions (id, user_id FK, started_at, ended_at, duration_minutes INT, activity_type ENUM: LESSON/EXERCISE/VOCABULARY/CONVERSATION/GRAMMAR, activity_id UUID NULL, xp_earned INT DEFAULT 0)
- learning_progress (id, user_id FK, metric_type ENUM: VOCABULARY_MASTERED/EXERCISES_COMPLETED/STREAK_DAYS/XP_TOTAL/LESSONS_COMPLETED, metric_value DECIMAL, recorded_date DATE, updated_at)
- UNIQUE constraint: (user_id, metric_type, recorded_date)

### V9__create_billing_and_audit.sql
Tables:
- subscriptions (id, user_id FK, plan ENUM: FREE/PRO/PREMIUM, status ENUM: ACTIVE/CANCELLED/EXPIRED, started_at, expires_at, stripe_subscription_id VARCHAR NULL, metadata JSONB)
- audit_logs (id, user_id FK NULL, action VARCHAR, entity_type VARCHAR, entity_id UUID NULL, old_value JSONB, new_value JSONB, ip_address VARCHAR, user_agent TEXT, created_at)

### V10__create_indexes.sql
Add indexes for:
- All FK columns
- users.email (UNIQUE)
- user_vocabularies(user_id, next_review_at) — for spaced repetition queries
- learning_progress(user_id, recorded_date)
- conversation_messages(conversation_id, created_at)
- ai_logs(created_at) — for cleanup jobs
- exercise_attempts(user_id, completed_at)

## Constraints
- Use gen_random_uuid() for UUID defaults
- All timestamps: TIMESTAMP WITH TIME ZONE
- JSONB columns have GIN index where queried
- Add meaningful COMMENTs on tables
- NO application-level default values in SQL — only DB-level defaults