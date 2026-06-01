package com.englishai.ai.service;

import com.englishai.ai.repository.AiLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenUsageTracker {

    private final AiLogRepository aiLogRepository;
    private static final int MAX_REQUESTS_PER_HOUR = 20;

    /**
     * Checks if a user can make an AI request.
     * The limit is MAX_REQUESTS_PER_HOUR (20) per hour.
     */
    public boolean canMakeRequest(UUID userId) {
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        long requests = aiLogRepository.countByUserIdAndCreatedAtAfter(userId, oneHourAgo);
        log.debug("User {} has made {} AI requests in the past hour.", userId, requests);
        return requests < MAX_REQUESTS_PER_HOUR;
    }

    /**
     * Aggregates total token usage by user + month for billing purposes.
     */
    public long sumTokensByUserAndMonth(UUID userId, YearMonth month) {
        LocalDateTime startOfMonth = month.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = month.atEndOfMonth().atTime(23, 59, 59, 999999999);

        Instant start = startOfMonth.toInstant(ZoneOffset.UTC);
        Instant end = endOfMonth.toInstant(ZoneOffset.UTC);

        return aiLogRepository.sumTotalTokensByUserIdAndCreatedAtBetween(userId, start, end);
    }
}
