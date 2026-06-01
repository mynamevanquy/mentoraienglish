package com.englishai.exercise.dto;

import com.englishai.common.enums.AttemptStatus;
import com.englishai.exercise.entity.ExerciseAttempt;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExerciseAttemptDto(
        UUID id,
        UUID exerciseId,
        String exerciseTitle,
        Instant startedAt,
        Instant completedAt,
        BigDecimal score,
        BigDecimal maxScore,
        Integer timeSpentSeconds,
        AttemptStatus status
) {
    public static ExerciseAttemptDto from(ExerciseAttempt attempt) {
        return new ExerciseAttemptDto(
                attempt.getId(),
                attempt.getExercise().getId(),
                attempt.getExercise().getTitle(),
                attempt.getStartedAt(),
                attempt.getCompletedAt(),
                attempt.getScore(),
                attempt.getMaxScore(),
                attempt.getTimeSpentSeconds(),
                attempt.getStatus());
    }
}
