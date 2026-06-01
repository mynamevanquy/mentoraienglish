package com.englishai.exercise.dto;

import com.englishai.exercise.entity.Exercise;

import java.util.Comparator;
import java.util.List;

public record ExerciseDetailDto(
        ExerciseDto exercise,
        List<ExerciseQuestionViewDto> questions
) {
    public static ExerciseDetailDto from(Exercise exercise) {
        List<ExerciseQuestionViewDto> questions = exercise.getQuestions().stream()
                .sorted(Comparator.comparingInt(q -> q.getOrderIndex()))
                .map(ExerciseQuestionViewDto::from)
                .toList();
        return new ExerciseDetailDto(ExerciseDto.from(exercise), questions);
    }
}
