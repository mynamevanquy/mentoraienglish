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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final ExerciseAttemptRepository exerciseAttemptRepository;
    private final ExerciseAnswerRepository exerciseAnswerRepository;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final com.englishai.ai.service.AiService aiService;
    private final ExerciseGrader exerciseGrader;
    private final ProgressService progressService;
    private final LearnerLevelService learnerLevelService;

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
    public AttemptResultDto submitAttempt(UUID userId, UUID attemptId, Map<UUID, String> submittedAnswers) {
        ExerciseAttempt attempt = findUserAttempt(attemptId, userId);
        requireInProgress(attempt);

        List<ExerciseQuestion> questions = attempt.getExercise().getQuestions().stream()
                .sorted(Comparator.comparingInt(ExerciseQuestion::getOrderIndex))
                .toList();
        validateCompleteSubmission(questions, submittedAnswers);

        if (!exerciseAnswerRepository.findByAttemptId(attemptId).isEmpty()) {
            throw new IllegalStateException("Bài luyện tập này đã có câu trả lời và không thể nộp lại.");
        }

        List<ExerciseAnswer> answers = questions.stream()
                .map(question -> gradeAnswer(attempt, question, submittedAnswers.get(question.getId()), userId))
                .toList();
        exerciseAnswerRepository.saveAll(answers);

        return completeAttempt(attempt, answers);
    }

    @Transactional
    public AttemptResultDto completeAttempt(UUID userId, UUID attemptId) {
        ExerciseAttempt attempt = findUserAttempt(attemptId, userId);
        requireInProgress(attempt);

        List<ExerciseAnswer> answers = exerciseAnswerRepository.findByAttemptId(attemptId);
        Set<UUID> answeredQuestionIds = answers.stream()
                .map(answer -> answer.getQuestion().getId())
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        int questionCount = attempt.getExercise().getQuestions().size();
        if (answeredQuestionIds.size() != questionCount) {
            throw new IllegalStateException(
                    "Bạn cần trả lời đủ " + questionCount + " câu trước khi nộp bài. Còn "
                            + (questionCount - answeredQuestionIds.size()) + " câu chưa trả lời.");
        }

        return completeAttempt(attempt, answers);
    }

    @Transactional
    public ExerciseDto generateAiExercise(UUID userId, GenerateExerciseRequest request) {
        com.englishai.common.enums.Level learnerLevel = learnerLevelService.determineLevel(userId);
        com.englishai.ai.dto.GenerateExerciseRequest aiRequest = new com.englishai.ai.dto.GenerateExerciseRequest(
                request.getTopic(),
                request.getExerciseType().name(),
                learnerLevel.name(),
                request.getQuestionCount());

        List<com.englishai.ai.dto.ExerciseQuestionDto> generatedQuestions = aiService.generateExercises(userId, aiRequest).join();
        validateGeneratedQuestions(generatedQuestions, request);

        Exercise exercise = Exercise.builder()
                .title("AI: " + request.getTopic())
                .description("Generated practice for " + request.getTopic())
                .exerciseType(request.getExerciseType())
                .source(ExerciseSource.AI_GENERATED)
                .difficultyLevel(learnerLevel)
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
        if (attempt.getStatus() != AttemptStatus.COMPLETED) {
            throw new IllegalStateException("Chỉ có thể xem kết quả sau khi đã nộp bài.");
        }
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

    private void validateCompleteSubmission(
            List<ExerciseQuestion> questions,
            Map<UUID, String> submittedAnswers) {
        Set<UUID> questionIds = questions.stream()
                .map(ExerciseQuestion::getId)
                .collect(Collectors.toSet());
        if (submittedAnswers == null
                || submittedAnswers.size() != questions.size()
                || !submittedAnswers.keySet().equals(questionIds)) {
            throw new IllegalStateException("Vui lòng trả lời đầy đủ tất cả câu hỏi trước khi nộp bài.");
        }
        if (submittedAnswers.values().stream().anyMatch(answer -> !StringUtils.hasText(answer))) {
            throw new IllegalStateException("Vui lòng trả lời đầy đủ tất cả câu hỏi trước khi nộp bài.");
        }
    }

    private ExerciseAnswer gradeAnswer(
            ExerciseAttempt attempt,
            ExerciseQuestion question,
            String userAnswer,
            UUID userId) {
        ExerciseGrader.GradingResult grading = exerciseGrader.grade(question, userAnswer, userId);
        return ExerciseAnswer.builder()
                .attempt(attempt)
                .question(question)
                .userAnswer(userAnswer)
                .isCorrect(grading.correct())
                .pointsEarned(grading.pointsEarned())
                .aiFeedback(grading.aiFeedback())
                .answeredAt(Instant.now())
                .build();
    }

    private AttemptResultDto completeAttempt(
            ExerciseAttempt attempt,
            List<ExerciseAnswer> answers) {
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
        progressService.recordStudySession(
                savedAttempt.getUser().getId(),
                ActivityType.EXERCISE,
                savedAttempt.getExercise().getId(),
                durationMinutes,
                xpEarned);

        return toAttemptResult(savedAttempt, answers);
    }

    private void validateGeneratedQuestions(
            List<com.englishai.ai.dto.ExerciseQuestionDto> generatedQuestions,
            GenerateExerciseRequest request) {
        if (generatedQuestions == null || generatedQuestions.size() != request.getQuestionCount()) {
            int actualCount = generatedQuestions == null ? 0 : generatedQuestions.size();
            throw new IllegalStateException(
                    "AI trả về " + actualCount + "/" + request.getQuestionCount() + " câu hỏi. Vui lòng thử lại.");
        }

        IntStream.range(0, generatedQuestions.size()).forEach(index -> {
            com.englishai.ai.dto.ExerciseQuestionDto question = generatedQuestions.get(index);
            if (question == null
                    || !StringUtils.hasText(question.questionText())
                    || !StringUtils.hasText(question.correctOption())
                    || !StringUtils.hasText(question.explanation())) {
                throw new IllegalStateException("Câu hỏi AI số " + (index + 1) + " thiếu nội dung bắt buộc.");
            }
            if (request.getExerciseType() == com.englishai.common.enums.ExerciseType.MULTIPLE_CHOICE) {
                if (question.options() == null || question.options().size() < 2) {
                    throw new IllegalStateException("Câu trắc nghiệm số " + (index + 1) + " không đủ lựa chọn.");
                }
                boolean answerInOptions = question.options().stream()
                        .filter(StringUtils::hasText)
                        .anyMatch(option -> option.trim().equalsIgnoreCase(question.correctOption().trim()));
                if (!answerInOptions) {
                    throw new IllegalStateException(
                            "Đáp án câu trắc nghiệm số " + (index + 1) + " không nằm trong các lựa chọn.");
                }
            }
        });
    }
}
