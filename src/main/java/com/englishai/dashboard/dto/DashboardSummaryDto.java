package com.englishai.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO representing the aggregated statistics shown on the dashboard.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryDto {
    private UUID userId;
    private int todayStudyMinutes;
    private int currentStreakDays;
    private long vocabularyMastered;
    private long vocabularyDueReview;
    private long exercisesCompletedThisWeek;
    private double averageExerciseScore;
    private long totalXp;
    private long activeConversations;
}
