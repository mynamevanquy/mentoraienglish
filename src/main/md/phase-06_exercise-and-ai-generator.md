You are a senior Java Spring Boot engineer.

Context: Spring Boot 3.2, Java 21, JPA, Thymeleaf, HTMX, Bootstrap 5

## Task
Generate complete exercise/ module including AI exercise generation.

## 1. ExerciseService.java
Methods:
- Page<ExerciseDto> getPublishedExercises(ExerciseFilter filter, Pageable pageable)
- ExerciseDetailDto getExerciseWithQuestions(UUID exerciseId)
- ExerciseAttemptDto startAttempt(UUID userId, UUID exerciseId)
- AttemptResultDto submitAttempt(UUID userId, UUID attemptId, Map<UUID, String> answers)
- AttemptResultDto completeAttempt(UUID userId, UUID attemptId)
- ExerciseDto generateAiExercise(UUID userId, GenerateExerciseRequest request) — calls AiService
- Page<ExerciseAttemptDto> getUserAttempts(UUID userId, Pageable pageable)

## 2. ExerciseGrader.java
Grade answers per type:
- MULTIPLE_CHOICE: exact match correct_answer
- FILL_BLANK: case-insensitive trim match, allow common typos (Levenshtein distance <= 1)
- SENTENCE_REORDER: compare word order array
- WRITING: call AiService.correctGrammar(), return score + feedback
- LISTENING: same as FILL_BLANK

## 3. GenerateExerciseRequest.java (DTO)
Fields: topic (String), exerciseType (ExerciseType), questionCount (int 1-20), relatedLessonId (UUID, optional)
Difficulty is determined by the server from the learner's completed-attempt count and average score.
Validation: @NotBlank topic, questionCount 1-20

## 4. ExerciseController.java
Endpoints:
- GET /exercises — list page
- GET /exercises/{id} — exercise detail (full page)
- POST /exercises/{id}/start — start attempt, redirect to attempt page
- GET /exercises/attempt/{attemptId} — attempt page
- POST /exercises/attempt/{attemptId}/submit — submit all answers once, complete, redirect to result page
- GET /exercises/attempt/{attemptId}/result — result page
- GET /exercises/generate — AI generator form page
- POST /exercises/generate — submit generation request, HTMX: show progress then redirect

## 5. Thymeleaf templates
exercises/index.html — exercise list with filters
exercises/detail.html — exercise info + start button
exercises/attempt.html — question-by-question UI
exercises/result.html — score, per-question breakdown, AI feedback
exercises/generate.html — AI exercise generator form

## AI generation flow:
1. User submits GenerateExerciseRequest
2. The server determines the learner's current level automatically
3. Controller calls ExerciseService.generateAiExercise() (async)
4. HTMX shows loading spinner via hx-trigger="load"
5. When complete, redirect to new exercise detail page
6. If AI fails, show error partial, offer retry

## Constraints
- exercise_attempts.status must be IN_PROGRESS to accept answers
- Once COMPLETED, attempt is immutable
- AI-generated exercises saved to DB before returning to user
- Writing type exercises: store ai_feedback in exercise_answers.ai_feedback column
