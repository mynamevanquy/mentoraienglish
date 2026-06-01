You are a senior Java Spring Boot engineer.

Context: Spring Boot 3.2, Java 21, JPA, Thymeleaf, HTMX, Bootstrap 5

## Task
Generate the complete vocabulary/ module with SM-2 spaced repetition algorithm.

## 1. SM2Algorithm.java
Implement SuperMemo 2 algorithm:
Input: quality (0-5), current easiness_factor, interval, repetitions
Output: next easiness_factor, next interval (days), next repetitions, next_review_at

Quality scale:
- 0: complete blackout
- 1: wrong, correct answer remembered
- 2: wrong, easy to recall
- 3: correct with difficulty
- 4: correct after hesitation
- 5: perfect recall

Formula (standard SM-2):
- If quality < 3: reset repetitions=0, interval=1
- Else: calculate new EF = EF + (0.1 - (5-q)*(0.08 + (5-q)*0.02)), min EF = 1.3

## 2. VocabularyService.java
Methods:
- Page<VocabularyDto> searchVocabularies(String keyword, String level, Pageable pageable)
- VocabularyDto getById(UUID id)
- List<UserVocabularyDto> getDueForReview(UUID userId, int limit) — SM-2 review queue
- UserVocabularyDto submitReview(UUID userId, UUID vocabularyId, int quality) — apply SM-2
- UserVocabularyDto addToUserList(UUID userId, UUID vocabularyId)
- VocabularyInfoDto getAiExplanation(UUID userId, UUID vocabularyId) — calls AiService
- Page<UserVocabularyDto> getUserVocabularies(UUID userId, Pageable pageable)
- Map<String, Long> getMasteryStats(UUID userId) — count by mastery_level

## 3. VocabularyController.java
Endpoints (all require auth):
- GET /vocabulary — main page (full page load)
- GET /vocabulary/list — HTMX partial: paginated vocabulary list
- GET /vocabulary/{id} — HTMX partial: vocabulary detail card
- POST /vocabulary/{id}/add — add to user list, return HTMX partial
- GET /vocabulary/review — review page
- GET /vocabulary/review/next — HTMX: get next vocabulary card for review
- POST /vocabulary/review/{id}/submit — submit review quality (0-5), return updated card
- GET /vocabulary/stats — HTMX partial: mastery stats widget

## 4. Thymeleaf templates
vocabulary/index.html — layout with search bar, list, sidebar stats
vocabulary/fragments/vocabulary-card.html — single vocabulary card (word, phonetic, definition, examples)
vocabulary/fragments/review-card.html — flashcard UI for review session
vocabulary/fragments/stats-widget.html — donut-style mastery breakdown
vocabulary/review.html — review session page

## HTMX patterns to use:
- hx-get with hx-target="#vocabulary-list" for pagination
- hx-post with hx-swap="outerHTML" for review submission
- hx-trigger="revealed" for lazy loading vocabulary cards

## Constraints
- Mastery level 0-5 maps to: New/Learning/Familiar/Good/Mastered/Expert
- getDueForReview must use DB query: next_review_at <= NOW() ORDER BY next_review_at ASC
- AI explanation must be cached in user_vocabularies.metadata JSONB to avoid repeated API calls