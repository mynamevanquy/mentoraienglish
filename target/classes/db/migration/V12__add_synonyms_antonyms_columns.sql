-- ============================================================
-- V12: Add synonyms/antonyms columns to vocabularies
-- ============================================================

ALTER TABLE vocabularies ADD COLUMN IF NOT EXISTS synonyms JSONB;
ALTER TABLE vocabularies ADD COLUMN IF NOT EXISTS antonyms JSONB;
