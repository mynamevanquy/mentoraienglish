package com.englishai.exercise.service;

import com.englishai.common.enums.AttemptStatus;
import com.englishai.common.enums.ExerciseType;
import com.englishai.common.enums.Level;
import com.englishai.exercise.config.AdaptiveLearningProperties;
import com.englishai.exercise.entity.Exercise;
import com.englishai.exercise.entity.ExerciseAttempt;
import com.englishai.exercise.enums.AssessmentStatus;
import com.englishai.exercise.enums.LearningSkill;
import com.englishai.exercise.repository.ExerciseAttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LearnerLevelServiceTest {

    private final ExerciseAttemptRepository repository = mock(ExerciseAttemptRepository.class);
    private AdaptiveLearningProperties properties;
    private LearnerLevelService service;

    @BeforeEach
    void setUp() {
        properties = new AdaptiveLearningProperties();
        service = new LearnerLevelService(repository, properties);
    }

    @Test
    void keepsNewLearnerUnassessedAtDefaultLevel() {
        UUID userId = UUID.randomUUID();
        when(repository.findAssessmentEvidence(userId, AttemptStatus.COMPLETED)).thenReturn(List.of());

        var profile = service.getProfile(userId);

        assertThat(profile.level()).isEqualTo(Level.BEGINNER);
        assertThat(profile.assessmentStatus()).isEqualTo(AssessmentStatus.UNASSESSED);
        assertThat(profile.confidencePercent()).isZero();
    }

    @Test
    void promotesToIntermediateAfterStrongMultiSkillFoundationEvidence() {
        UUID userId = UUID.randomUUID();
        List<ExerciseAttempt> attempts = List.of(
                attempt(Level.BEGINNER, ExerciseType.MULTIPLE_CHOICE, 9, 10),
                attempt(Level.BEGINNER, ExerciseType.FILL_BLANK, 8, 10),
                attempt(Level.BEGINNER, ExerciseType.SENTENCE_REORDER, 9, 10),
                attempt(Level.BEGINNER, ExerciseType.LISTENING, 8, 10),
                attempt(Level.BEGINNER, ExerciseType.LISTENING, 9, 10));
        when(repository.findAssessmentEvidence(userId, AttemptStatus.COMPLETED)).thenReturn(attempts);

        var profile = service.getProfile(userId);

        assertThat(profile.level()).isEqualTo(Level.INTERMEDIATE);
        assertThat(profile.assessmentStatus()).isEqualTo(AssessmentStatus.PROVISIONAL);
        assertThat(profile.skillScores()).containsKeys(LearningSkill.LANGUAGE_USE, LearningSkill.LISTENING);
    }

    @Test
    void doesNotPromoteFromOnlyOneSkillArea() {
        UUID userId = UUID.randomUUID();
        List<ExerciseAttempt> attempts = List.of(
                attempt(Level.BEGINNER, ExerciseType.MULTIPLE_CHOICE, 10, 10),
                attempt(Level.BEGINNER, ExerciseType.FILL_BLANK, 10, 10),
                attempt(Level.BEGINNER, ExerciseType.SENTENCE_REORDER, 10, 10),
                attempt(Level.BEGINNER, ExerciseType.MULTIPLE_CHOICE, 10, 10),
                attempt(Level.BEGINNER, ExerciseType.FILL_BLANK, 10, 10));
        when(repository.findAssessmentEvidence(userId, AttemptStatus.COMPLETED)).thenReturn(attempts);

        assertThat(service.determineLevel(userId)).isEqualTo(Level.BEGINNER);
    }

    @Test
    void promotesToAdvancedOnlyFromIntermediateOrHigherEvidenceAcrossAllSkills() {
        UUID userId = UUID.randomUUID();
        List<ExerciseAttempt> attempts = List.of(
                attempt(Level.INTERMEDIATE, ExerciseType.MULTIPLE_CHOICE, 9, 10),
                attempt(Level.INTERMEDIATE, ExerciseType.FILL_BLANK, 8, 10),
                attempt(Level.INTERMEDIATE, ExerciseType.SENTENCE_REORDER, 9, 10),
                attempt(Level.INTERMEDIATE, ExerciseType.LISTENING, 8, 10),
                attempt(Level.INTERMEDIATE, ExerciseType.LISTENING, 9, 10),
                attempt(Level.INTERMEDIATE, ExerciseType.WRITING, 8, 10),
                attempt(Level.INTERMEDIATE, ExerciseType.WRITING, 9, 10),
                attempt(Level.ADVANCED, ExerciseType.WRITING, 8, 10));
        when(repository.findAssessmentEvidence(userId, AttemptStatus.COMPLETED)).thenReturn(attempts);

        assertThat(service.determineLevel(userId)).isEqualTo(Level.ADVANCED);
    }

    @Test
    void cannotReachAdvancedByRepeatingOnlyBeginnerDifficulty() {
        UUID userId = UUID.randomUUID();
        List<ExerciseAttempt> attempts = new ArrayList<>();
        for (int index = 0; index < 12; index++) {
            ExerciseType type = ExerciseType.values()[index % ExerciseType.values().length];
            attempts.add(attempt(Level.BEGINNER, type, 10, 10));
        }
        when(repository.findAssessmentEvidence(userId, AttemptStatus.COMPLETED)).thenReturn(attempts);

        assertThat(service.determineLevel(userId)).isEqualTo(Level.INTERMEDIATE);
    }

    @Test
    void countsOnlyLatestAttemptForEachExercise() {
        UUID userId = UUID.randomUUID();
        UUID duplicatedExerciseId = UUID.randomUUID();
        List<ExerciseAttempt> attempts = List.of(
                attempt(duplicatedExerciseId, Level.BEGINNER, ExerciseType.LISTENING, 9, 10, 30),
                attempt(duplicatedExerciseId, Level.BEGINNER, ExerciseType.LISTENING, 10, 10, 30),
                attempt(Level.BEGINNER, ExerciseType.MULTIPLE_CHOICE, 9, 10),
                attempt(Level.BEGINNER, ExerciseType.FILL_BLANK, 9, 10),
                attempt(Level.BEGINNER, ExerciseType.SENTENCE_REORDER, 9, 10));
        when(repository.findAssessmentEvidence(userId, AttemptStatus.COMPLETED)).thenReturn(attempts);

        var profile = service.getProfile(userId);

        assertThat(profile.completedAttempts()).isEqualTo(4);
        assertThat(profile.level()).isEqualTo(Level.BEGINNER);
    }

    @Test
    void excludesImplausiblyFastAttemptFromEvidence() {
        UUID userId = UUID.randomUUID();
        List<ExerciseAttempt> attempts = List.of(
                attempt(Level.BEGINNER, ExerciseType.MULTIPLE_CHOICE, 9, 10),
                attempt(Level.BEGINNER, ExerciseType.FILL_BLANK, 9, 10),
                attempt(Level.BEGINNER, ExerciseType.SENTENCE_REORDER, 9, 10),
                attempt(Level.BEGINNER, ExerciseType.LISTENING, 9, 10),
                attempt(UUID.randomUUID(), Level.BEGINNER, ExerciseType.LISTENING, 10, 10, 1));
        when(repository.findAssessmentEvidence(userId, AttemptStatus.COMPLETED)).thenReturn(attempts);

        var profile = service.getProfile(userId);

        assertThat(profile.completedAttempts()).isEqualTo(4);
        assertThat(profile.level()).isEqualTo(Level.BEGINNER);
    }

    @Test
    void marksProfileEstablishedOnlyWithEnoughEvidenceAcrossAllSupportedSkills() {
        UUID userId = UUID.randomUUID();
        List<ExerciseAttempt> attempts = new ArrayList<>();
        for (int index = 0; index < 10; index++) {
            ExerciseType type = switch (index % 3) {
                case 0 -> ExerciseType.MULTIPLE_CHOICE;
                case 1 -> ExerciseType.LISTENING;
                default -> ExerciseType.WRITING;
            };
            attempts.add(attempt(Level.INTERMEDIATE, type, 8, 10));
        }
        when(repository.findAssessmentEvidence(userId, AttemptStatus.COMPLETED)).thenReturn(attempts);

        assertThat(service.getProfile(userId).assessmentStatus()).isEqualTo(AssessmentStatus.ESTABLISHED);
    }

    @Test
    void canDisableAutomaticAssessment() {
        UUID userId = UUID.randomUUID();
        properties.setEnabled(false);
        properties.setDefaultLevel(Level.INTERMEDIATE);

        var profile = service.getProfile(userId);

        assertThat(profile.level()).isEqualTo(Level.INTERMEDIATE);
        assertThat(profile.assessmentStatus()).isEqualTo(AssessmentStatus.UNASSESSED);
    }

    private ExerciseAttempt attempt(Level level, ExerciseType type, int score, int maxScore) {
        return attempt(UUID.randomUUID(), level, type, score, maxScore, maxScore * 5);
    }

    private ExerciseAttempt attempt(
            UUID exerciseId,
            Level level,
            ExerciseType type,
            int score,
            int maxScore,
            int timeSpentSeconds) {
        Exercise exercise = Exercise.builder()
                .difficultyLevel(level)
                .exerciseType(type)
                .title("Assessment exercise")
                .build();
        exercise.setId(exerciseId);
        return ExerciseAttempt.builder()
                .id(UUID.randomUUID())
                .exercise(exercise)
                .status(AttemptStatus.COMPLETED)
                .score(BigDecimal.valueOf(score))
                .maxScore(BigDecimal.valueOf(maxScore))
                .timeSpentSeconds(timeSpentSeconds)
                .completedAt(Instant.now())
                .build();
    }
}
