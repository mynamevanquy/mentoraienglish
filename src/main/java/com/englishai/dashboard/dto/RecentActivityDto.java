package com.englishai.dashboard.dto;

import com.englishai.common.enums.ActivityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing a single recent activity item for the dashboard feed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentActivityDto {
    private ActivityType activityType;
    private UUID activityId;
    private Instant startedAt;
    private Integer durationMinutes;
    private Integer xpEarned;
}
