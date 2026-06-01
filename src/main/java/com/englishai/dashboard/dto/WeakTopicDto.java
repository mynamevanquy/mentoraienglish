package com.englishai.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a grammar topic where the user scored poorly.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeakTopicDto {
    private String topicTitle;
    private double averageScore;
    private long attemptCount;
}
