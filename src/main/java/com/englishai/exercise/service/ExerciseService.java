package com.englishai.exercise.service;

import com.englishai.common.enums.AttemptStatus;
import com.englishai.common.enums.ExerciseSource;
import com.englishai.exercise.dto.*;
import com.englishai.exercise.entity.Exercise;
import com.englishai.exercise.entity.ExerciseAnswer;
import com.englishai.exercise.entity.ExerciseAttempt;
import com.englishai.exercise.entity.ExerciseQuestion;
import com.englishai.exercise.repository.ExerciseAnswerRepository;
import com.englishai.exercise.repository.ExerciseAttemptRepository;
import com.englishai.exercise.repository.ExerciseQuestionRepository;
import com.englishai.exercise.repository.ExerciseRepository;
import com.englishai.lesson.repository.LessonRepository;
import com.englishai.user.entity.User;
import com.englishai.user.repository.UserRepository;
import com.englishai.common.enums.ActivityType;
import com.englishai.progress.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final ExerciseQuestionRepository exerciseQuestionRepository;
    private final ExerciseAttemptRepository exerciseAttemptRepository;
    private final ExerciseAnswerRepository exerciseAnswerRepository;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final com.englishai.ai.service.AiService aiService;
    private final ExerciseGrader exerciseGrader;
    private final ProgressService progressService;

    @Transactional(readOnly = true)
    public Page<ExerciseDto> getPublishedExercises(ExerciseFilter filter, Pageable pageable) {
        String keyword = filter != null && StringUtils.hasText(filter.keyword()) ? filter.keyword().trim() : null;
        return exerciseRepository.searchPublished(
                keyword,
                filter == null ? null : filter.exerciseType(),
                filter == null ? null : filter.difficulty(),
                pageable).map(ExerciseDto::from);
    }

    @Transactional(readOnly = true)
    public ExerciseDetailDto getExerciseWithQuestions(UUID exerciseId) {
        Exercise exercise = findExerciseWithQuestions(exerciseId);
        return ExerciseDetailDto.from(exercise);
    }

    @Transactional
    public ExerciseAttemptDto startAttempt(UUID userId, UUID exerciseId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));
        Exercise exercise = findExerciseWithQuestions(exerciseId);
        if (!exercise.isPublished()) {
            throw new IllegalArgumentException("Exercise is not published.");
        }

        int maxScore = exercise.getQuestions().stream().mapToInt(ExerciseQuestion::getPoints).sum();
        ExerciseAttempt attempt = ExerciseAttempt.builder()
                .user(user)
                .exercise(exercise)
                .startedAt(Instant.now())
                .status(AttemptStatus.IN_PROGRESS)
                .maxScore(BigDecimal.valueOf(maxScore))
                .build();
        return ExerciseAttemptDto.from(exerciseAttemptRepository.save(attempt));
    }

    @Transactional
    public SubmitAnswerResult submitAnswer(UUID userId, UUID attemptId, UUID questionId, String answer) {
        ExerciseAttempt attempt = findUserAttempt(attemptId, userId);
        requireInProgress(attempt);

        ExerciseQuestion question = exerciseQuestionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));
        if (!question.getExercise().getId().equals(attempt.getExercise().getId())) {
            throw new IllegalArgumentException("Question does not belong to this attempt.");
        }

        ExerciseGrader.GradingResult grading = exerciseGrader.grade(question, answer, userId);
        ExerciseAnswer exerciseAnswer = exerciseAnswerRepository.findByAttemptIdAndQuestionId(attemptId, questionId)
                .orElseGet(() -> ExerciseAnswer.builder()
                        .attempt(attempt)
                        .question(question)
                        .build());

        exerciseAnswer.setUserAnswer(answer);
        exerciseAnswer.setCorrect(grading.correct());
        exerciseAnswer.setPointsEarned(grading.pointsEarned());
        exerciseAnswer.setAiFeedback(grading.aiFeedback());
        exerciseAnswer.setAnsweredAt(Instant.now());
        exerciseAnswerRepository.save(exerciseAnswer);

        return new SubmitAnswerResult(
                questionId,
                answer,
                grading.correct(),
                grading.pointsEarned(),
                grading.maxPoints(),
                grading.explanation(),
                grading.aiFeedback());
    }

    @Transactional
    public AttemptResultDto completeAttempt(UUID userId, UUID attemptId) {
        ExerciseAttempt attempt = findUserAttempt(attemptId, userId);
        requireInProgress(attempt);

        List<ExerciseAnswer> answers = exerciseAnswerRepository.findByAttemptId(attemptId);
        int score = answers.stream().mapToInt(ExerciseAnswer::getPointsEarned).sum();
        int maxScore = attempt.getExercise().getQuestions().stream().mapToInt(ExerciseQuestion::getPoints).sum();

        attempt.setScore(BigDecimal.valueOf(score));
        attempt.setMaxScore(BigDecimal.valueOf(maxScore));
        attempt.setCompletedAt(Instant.now());
        attempt.setTimeSpentSeconds((int) Duration.between(attempt.getStartedAt(), attempt.getCompletedAt()).toSeconds());
        attempt.setStatus(AttemptStatus.COMPLETED);
        ExerciseAttempt savedAttempt = exerciseAttemptRepository.save(attempt);

        int durationMinutes = Math.max(1, savedAttempt.getTimeSpentSeconds() / 60);
        int xpEarned = maxScore > 0 ? (int) Math.round(5.0 * ((double) score / maxScore) * 10.0) : 0;
        progressService.recordStudySession(userId, ActivityType.EXERCISE, savedAttempt.getExercise().getId(), durationMinutes, xpEarned);

        return toAttemptResult(savedAttempt, answers);
    }

    @Transactional
    public ExerciseDto generateAiExercise(UUID userId, GenerateExerciseRequest request) {
        com.englishai.ai.dto.GenerateExerciseRequest aiRequest = new com.englishai.ai.dto.GenerateExerciseRequest(
                request.getTopic(),
                request.getExerciseType().name(),
                request.getDifficulty().name(),
                request.getQuestionCount());

        List<com.englishai.ai.dto.ExerciseQuestionDto> generatedQuestions = aiService.generateExercises(userId, aiRequest).join();
        if (generatedQuestions.isEmpty()) {
            throw new IllegalStateException("AI did not return any exercise questions.");
        }

        Exercise exercise = Exercise.builder()
                .title("AI: " + request.getTopic())
                .description("Generated practice for " + request.getTopic())
                .exerciseType(request.getExerciseType())
                .source(ExerciseSource.AI_GENERATED)
                .difficultyLevel(request.getDifficulty())
                .isPublished(true)
                .metadata(Map.of("topic", request.getTopic(), "generatedByUserId", userId.toString()))
                .build();

        if (request.getRelatedLessonId() != null) {
            lessonRepository.findById(request.getRelatedLessonId()).ifPresent(exercise::setRelatedLesson);
        }

        int order = 0;
        for (com.englishai.ai.dto.ExerciseQuestionDto generated : generatedQuestions) {
            ExerciseQuestion question = ExerciseQuestion.builder()
                    .questionText(generated.questionText())
                    .questionType(request.getExerciseType())
                    .options(generated.options())
                    .correctAnswer(generated.correctOption())
                    .explanation(generated.explanation())
                    .orderIndex(order++)
                    .points(1)
                    .build();
            exercise.addQuestion(question);
        }

        return ExerciseDto.from(exerciseRepository.save(exercise));
    }

    @Transactional(readOnly = true)
    public Page<ExerciseAttemptDto> getUserAttempts(UUID userId, Pageable pageable) {
        return exerciseAttemptRepository.findByUserIdOrderByStartedAtDesc(userId, pageable).map(ExerciseAttemptDto::from);
    }

    @Transactional(readOnly = true)
    public ExerciseAttemptDto getAttempt(UUID userId, UUID attemptId) {
        return ExerciseAttemptDto.from(findUserAttempt(attemptId, userId));
    }

    @Transactional(readOnly = true)
    public AttemptResultDto getAttemptResult(UUID userId, UUID attemptId) {
        ExerciseAttempt attempt = findUserAttempt(attemptId, userId);
        List<ExerciseAnswer> answers = exerciseAnswerRepository.findByAttemptId(attemptId);
        return toAttemptResult(attempt, answers);
    }

    private AttemptResultDto toAttemptResult(ExerciseAttempt attempt, List<ExerciseAnswer> answers) {
        BigDecimal score = attempt.getScore() == null ? BigDecimal.ZERO : attempt.getScore();
        BigDecimal maxScore = attempt.getMaxScore() == null ? BigDecimal.ZERO : attempt.getMaxScore();
        double percentage = maxScore.compareTo(BigDecimal.ZERO) == 0
                ? 0
                : score.multiply(BigDecimal.valueOf(100)).divide(maxScore, 2, RoundingMode.HALF_UP).doubleValue();

        List<AttemptAnswerDto> answerDtos = answers.stream()
                .sorted(Comparator.comparing(a -> a.getQuestion().getOrderIndex()))
                .map(AttemptAnswerDto::from)
                .toList();

        return new AttemptResultDto(attempt.getId(), ExerciseDto.from(attempt.getExercise()), score, maxScore, percentage, answerDtos);
    }

    private Exercise findExerciseWithQuestions(UUID exerciseId) {
        return exerciseRepository.findWithQuestionsById(exerciseId)
                .orElseThrow(() -> new IllegalArgumentException("Exercise not found: " + exerciseId));
    }

    private ExerciseAttempt findUserAttempt(UUID attemptId, UUID userId) {
        return exerciseAttemptRepository.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));
    }

    private void requireInProgress(ExerciseAttempt attempt) {
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new IllegalStateException("This attempt is already completed or no longer accepts answers.");
        }
    }
}
