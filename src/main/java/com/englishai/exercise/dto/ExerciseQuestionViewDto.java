package com.englishai.exercise.dto;

import com.englishai.common.enums.ExerciseType;
import com.englishai.exercise.entity.ExerciseQuestion;

import java.util.List;
import java.util.UUID;

public record ExerciseQuestionViewDto(
        UUID id,
        String questionText,
        ExerciseType questionType,
        List<String> options,
        String correctAnswer,
        String explanation,
        int orderIndex,
        int points
) {
    public static ExerciseQuestionViewDto from(ExerciseQuestion question) {
        return new ExerciseQuestionViewDto(
                question.getId(),
                question.getQuestionText(),
                question.getQuestionType(),
                question.getOptions(),
                question.getCorrectAnswer(),
                question.getExplanation(),
                question.getOrderIndex(),
                question.getPoints());
    }
}
