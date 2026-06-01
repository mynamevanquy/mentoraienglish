package com.englishai.exercise.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AttemptResultDto(
        UUID attemptId,
        ExerciseDto exercise,
        BigDecimal score,
        BigDecimal maxScore,
        double percentage,
        List<AttemptAnswerDto> answers
) {
}
