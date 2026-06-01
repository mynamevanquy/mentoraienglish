package com.englishai.dashboard.dto;

public interface DashboardSummaryProjection {
    Number getTodayStudyMinutes();

    Number getCurrentStreakDays();

    Number getVocabularyMastered();

    Number getVocabularyDueReview();

    Number getExercisesCompletedThisWeek();

    Number getAverageExerciseScore();

    Number getTotalXp();

    Number getActiveConversations();
}
