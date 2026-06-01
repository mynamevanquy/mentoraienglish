package com.englishai.progress.dto;

import com.englishai.common.enums.MetricType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * DTO representing progress history for a specific metric over a date range.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgressHistoryDto {
    private MetricType metricType;
    private LocalDate from;
    private LocalDate to;
    private Map<LocalDate, BigDecimal> dailyValues;
}
