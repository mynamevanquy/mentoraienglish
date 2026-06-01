package com.englishai.vocabulary.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyStatsDto {
    private long totalWords;
    private long masteredWords;
    private long dueReviewCount;
    private long totalReviews;
    private Map<String, Long> masteryDistribution;
    private Map<LocalDate, Long> wordsAddedOverTime;
    private Map<LocalDate, Long> reviewsOverTime;
}
