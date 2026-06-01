package com.englishai.exercise.dto;

import com.englishai.common.enums.ExerciseSource;
import com.englishai.common.enums.ExerciseType;
import com.englishai.common.enums.Level;
import com.englishai.exercise.entity.Exercise;

import java.util.UUID;

public record ExerciseDto(
        UUID id,
        String title,
        String description,
        ExerciseType exerciseType,
        ExerciseSource source,
        Level difficultyLevel,
        Integer timeLimitSeconds,
        int questionCount
) {
    public static ExerciseDto from(Exercise exercise) {
        int count = exercise.getQuestions() == null ? 0 : exercise.getQuestions().size();
        return new ExerciseDto(
                exercise.getId(),
                exercise.getTitle(),
                exercise.getDescription(),
                exercise.getExerciseType(),
                exercise.getSource(),
                exercise.getDifficultyLevel(),
                exercise.getTimeLimitSeconds(),
                count);
    }
}
