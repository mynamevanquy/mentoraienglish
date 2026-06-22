package com.englishai.exercise.dto;

import com.englishai.exercise.entity.ExerciseAnswer;

import java.util.UUID;

public record AttemptAnswerDto(
        UUID questionId,
        String questionText,
        String userAnswer,
        String correctAnswer,
        boolean correct,
        int pointsEarned,
        int maxPoints,
        String explanation,
        String aiFeedback
) {
    public static AttemptAnswerDto from(ExerciseAnswer answer) {
        return new AttemptAnswerDto(
                answer.getQuestion().getId(),
                answer.getQuestion().getQuestionText(),
                answer.getUserAnswer(),
                answer.getQuestion().getCorrectAnswer(),
                answer.isCorrect(),
                answer.getPointsEarned(),
                answer.getQuestion().getPoints(),
                answer.getQuestion().getExplanation(),
                answer.getAiFeedback());
    }
}
