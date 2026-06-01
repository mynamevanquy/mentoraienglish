package com.englishai.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.Map;

/**
 * DTO representing weekly study minutes data for a bar chart.
 * The map key is the date (ISO string) and the value is minutes studied on that day.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyProgressDto {
    private Map<LocalDate, Integer> minutesPerDay;
}
