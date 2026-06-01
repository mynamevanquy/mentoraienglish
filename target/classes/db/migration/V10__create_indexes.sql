-- ============================================================
-- V11: Create Indexes for Performance optimization
-- ============================================================

-- Foreign Keys indexes
CREATE INDEX IF NOT EXISTS idx_lessons_course_id ON lessons (course_id);
CREATE INDEX IF NOT EXISTS idx_lesson_contents_lesson_id ON lesson_contents (lesson_id);
CREATE INDEX IF NOT EXISTS idx_user_vocabularies_user_id ON user_vocabularies (user_id);
CREATE INDEX IF NOT EXISTS idx_user_vocabularies_vocabulary_id ON user_vocabularies (vocabulary_id);
CREATE INDEX IF NOT EXISTS idx_exercises_lesson_id ON exercises (related_lesson_id);
CREATE INDEX IF NOT EXISTS idx_exercises_grammar_id ON exercises (related_grammar_id);
CREATE INDEX IF NOT EXISTS idx_exercise_questions_exercise_id ON exercise_questions (exercise_id);
CREATE INDEX IF NOT EXISTS idx_exercise_attempts_user_id ON exercise_attempts (user_id);
CREATE INDEX IF NOT EXISTS idx_exercise_attempts_exercise_id ON exercise_attempts (exercise_id);
CREATE INDEX IF NOT EXISTS idx_exercise_answers_attempt_id ON exercise_answers (attempt_id);
CREATE INDEX IF NOT EXISTS idx_exercise_answers_question_id ON exercise_answers (question_id);
CREATE INDEX IF NOT EXISTS idx_conversations_user_id ON conversations (user_id);
CREATE INDEX IF NOT EXISTS idx_conversation_messages_conversation_id ON conversation_messages (conversation_id);
CREATE INDEX IF NOT EXISTS idx_study_sessions_user_id ON study_sessions (user_id);
CREATE INDEX IF NOT EXISTS idx_learning_progress_user_id ON learning_progress (user_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id ON subscriptions (user_id);

-- Spaced Repetition query index
CREATE INDEX IF NOT EXISTS idx_user_vocabularies_review ON user_vocabularies (user_id, next_review_at);

-- Progress dashboard queries
CREATE INDEX IF NOT EXISTS idx_learning_progress_date ON learning_progress (user_id, recorded_date);

-- Conversation history queries
CREATE INDEX IF NOT EXISTS idx_conversation_messages_created ON conversation_messages (conversation_id, created_at);

-- Attempts lookup
CREATE INDEX IF NOT EXISTS idx_exercise_attempts_completed ON exercise_attempts (user_id, completed_at);

-- JSONB index for metadata (GIN index where queried)
CREATE INDEX IF NOT EXISTS idx_user_vocabularies_tags ON vocabularies USING gin (tags);
CREATE INDEX IF NOT EXISTS idx_exercises_metadata ON exercises USING gin (metadata);
