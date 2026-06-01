-- ============================================================
-- V11: Add metadata JSONB column to user_vocabularies
--      Used to cache AI-generated explanations per user/word
-- ============================================================

ALTER TABLE user_vocabularies
    ADD COLUMN IF NOT EXISTS metadata JSONB;

COMMENT ON COLUMN user_vocabularies.metadata
    IS 'Cached AI explanation (word, ipa, definition, examples, synonyms, antonyms) to avoid repeated API calls';
