package com.englishai.exercise.dto;

import com.englishai.common.enums.Level;
import com.englishai.exercise.enums.AssessmentStatus;
import com.englishai.exercise.enums.LearningSkill;

import java.util.Map;

public record LearnerProfileDto(
        Level level,
        long completedAttempts,
        double averageScore,
        int attemptsToNextLevel,
        double scoreRequiredForNextLevel,
        AssessmentStatus assessmentStatus,
        int confidencePercent,
        Map<LearningSkill, Double> skillScores,
        String recommendationReason
) {
}
