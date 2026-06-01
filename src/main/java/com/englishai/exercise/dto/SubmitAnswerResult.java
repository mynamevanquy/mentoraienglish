package com.englishai.exercise.dto;

import java.util.UUID;

public record SubmitAnswerResult(
        UUID questionId,
        String userAnswer,
        boolean correct,
        int pointsEarned,
        int maxPoints,
        String explanation,
        String aiFeedback
) {
}
