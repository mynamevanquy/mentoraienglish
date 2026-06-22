package com.englishai.exercise.service;

import com.englishai.ai.service.AiService;
import com.englishai.common.enums.AttemptStatus;
import com.englishai.common.enums.ExerciseType;
import com.englishai.exercise.entity.Exercise;
import com.englishai.exercise.entity.ExerciseAnswer;
import com.englishai.exercise.entity.ExerciseAttempt;
import com.englishai.exercise.entity.ExerciseQuestion;
import com.englishai.exercise.repository.ExerciseAnswerRepository;
import com.englishai.exercise.repository.ExerciseAttemptRepository;
import com.englishai.exercise.repository.ExerciseRepository;
import com.englishai.lesson.repository.LessonRepository;
import com.englishai.progress.service.ProgressService;
import com.englishai.user.entity.User;
import com.englishai.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExerciseServiceTest {

    @Mock private ExerciseRepository exerciseRepository;
    @Mock private ExerciseAttemptRepository exerciseAttemptRepository;
    @Mock private ExerciseAnswerRepository exerciseAnswerRepository;
    @Mock private UserRepository userRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private AiService aiService;
    @Mock private ExerciseGrader exerciseGrader;
    @Mock private ProgressService progressService;
    @Mock private LearnerLevelService learnerLevelService;

    private ExerciseService service;

    @BeforeEach
    void setUp() {
        service = new ExerciseService(
                exerciseRepository,
                exerciseAttemptRepository,
                exerciseAnswerRepository,
                userRepository,
                lessonRepository,
                aiService,
                exerciseGrader,
                progressService,
                learnerLevelService);
    }

    @Test
    void rejectsCompletingAttemptWithUnansweredQuestions() {
        UUID userId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        Exercise exercise = Exercise.builder().title("Test").questions(new java.util.ArrayList<>()).build();
        ExerciseQuestion first = question(UUID.randomUUID(), exercise);
        question(UUID.randomUUID(), exercise);
        ExerciseAttempt attempt = attempt(attemptId, userId, exercise, AttemptStatus.IN_PROGRESS);

        when(exerciseAttemptRepository.findByIdAndUserId(attemptId, userId)).thenReturn(Optional.of(attempt));
        when(exerciseAnswerRepository.findByAttemptId(attemptId))
                .thenReturn(List.of(ExerciseAnswer.builder().attempt(attempt).question(first).build()));

        assertThatThrownBy(() -> service.completeAttempt(userId, attemptId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("1 câu chưa trả lời");

        verify(exerciseAttemptRepository, never()).save(attempt);
    }

    @Test
    void rejectsViewingResultBeforeCompletion() {
        UUID userId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        ExerciseAttempt attempt = attempt(
                attemptId,
                userId,
                Exercise.builder().title("Test").build(),
                AttemptStatus.IN_PROGRESS);
        when(exerciseAttemptRepository.findByIdAndUserId(attemptId, userId)).thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> service.getAttemptResult(userId, attemptId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sau khi đã nộp bài");
    }

    @Test
    void rejectsSubmittingTheWholeAttemptWithMissingAnswers() {
        UUID userId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        Exercise exercise = Exercise.builder().title("Test").questions(new java.util.ArrayList<>()).build();
        ExerciseQuestion first = question(UUID.randomUUID(), exercise);
        question(UUID.randomUUID(), exercise);
        ExerciseAttempt attempt = attempt(attemptId, userId, exercise, AttemptStatus.IN_PROGRESS);

        when(exerciseAttemptRepository.findByIdAndUserId(attemptId, userId)).thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> service.submitAttempt(
                userId,
                attemptId,
                Map.of(first.getId(), "answer")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("đầy đủ tất cả câu hỏi");

        verify(exerciseAnswerRepository, never()).saveAll(anyList());
    }

    @Test
    void rejectsSubmittingTheWholeAttemptAgainWhenAnswersAlreadyExist() {
        UUID userId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        Exercise exercise = Exercise.builder().title("Test").questions(new java.util.ArrayList<>()).build();
        ExerciseQuestion question = question(UUID.randomUUID(), exercise);
        ExerciseAttempt attempt = attempt(attemptId, userId, exercise, AttemptStatus.IN_PROGRESS);

        when(exerciseAttemptRepository.findByIdAndUserId(attemptId, userId)).thenReturn(Optional.of(attempt));
        when(exerciseAnswerRepository.findByAttemptId(attemptId))
                .thenReturn(List.of(ExerciseAnswer.builder().attempt(attempt).question(question).build()));

        assertThatThrownBy(() -> service.submitAttempt(
                userId,
                attemptId,
                Map.of(question.getId(), "answer")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("không thể nộp lại");

        verify(exerciseGrader, never()).grade(question, "answer", userId);
    }

    private ExerciseQuestion question(UUID questionId, Exercise exercise) {
        ExerciseQuestion question = ExerciseQuestion.builder()
                .id(questionId)
                .questionType(ExerciseType.FILL_BLANK)
                .correctAnswer("answer")
                .points(1)
                .build();
        exercise.addQuestion(question);
        return question;
    }

    private ExerciseAttempt attempt(
            UUID attemptId,
            UUID userId,
            Exercise exercise,
            AttemptStatus status) {
        User user = User.builder().email("student@example.com").build();
        user.setId(userId);
        return ExerciseAttempt.builder()
                .id(attemptId)
                .user(user)
                .exercise(exercise)
                .status(status)
                .build();
    }
}
