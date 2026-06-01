package com.englishai.progress.service;

import com.englishai.common.enums.ActivityType;
import com.englishai.common.enums.MetricType;
import com.englishai.progress.dto.ProgressHistoryDto;
import com.englishai.progress.entity.LearningProgress;
import com.englishai.progress.entity.StudySession;
import com.englishai.progress.repository.LearningProgressRepository;
import com.englishai.progress.repository.StudySessionRepository;
import com.englishai.user.entity.User;
import com.englishai.user.repository.UserRepository;
import com.englishai.vocabulary.repository.UserVocabularyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final StudySessionRepository studySessionRepository;
    private final LearningProgressRepository learningProgressRepository;
    private final UserRepository userRepository;
    private final UserVocabularyRepository userVocabularyRepository;

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Transactional
    public void recordStudySession(UUID userId, ActivityType type, UUID activityId, int durationMinutes, int xpEarned) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Instant now = Instant.now();
        StudySession session = StudySession.builder()
                .user(user)
                .activityType(type)
                .activityId(activityId)
                .durationMinutes(durationMinutes)
                .xpEarned(xpEarned)
                .startedAt(now.minus(Duration.ofMinutes(durationMinutes)))
                .endedAt(now)
                .build();

        studySessionRepository.save(session);

        // Update daily progress metrics
        updateDailyProgress(userId, MetricType.XP_TOTAL, BigDecimal.valueOf(xpEarned));

        if (type == ActivityType.LESSON) {
            updateDailyProgress(userId, MetricType.LESSONS_COMPLETED, BigDecimal.ONE);
        } else if (type == ActivityType.EXERCISE) {
            updateDailyProgress(userId, MetricType.EXERCISES_COMPLETED, BigDecimal.ONE);
        } else if (type == ActivityType.VOCABULARY) {
            long masteredCount = userVocabularyRepository.countByUserIdAndMasteryLevelGreaterThanEqual(userId, 4);
            updateDailyProgress(userId, MetricType.VOCABULARY_MASTERED, BigDecimal.valueOf(masteredCount));
        }

        // Recalculate streak and update
        int streak = calculateStreakDays(userId);
        updateDailyProgress(userId, MetricType.STREAK_DAYS, BigDecimal.valueOf(streak));
    }

    @Transactional
    public void updateDailyProgress(UUID userId, MetricType metric, BigDecimal value) {
        LocalDate today = LocalDate.now(DEFAULT_ZONE);
        Instant now = Instant.now();

        // Streak and Mastery metrics should be overwritten. Others (XP, completed counts) are accumulated.
        if (metric == MetricType.STREAK_DAYS || metric == MetricType.VOCABULARY_MASTERED) {
            learningProgressRepository.upsertDailyProgressOverwrite(
                    UUID.randomUUID(),
                    userId,
                    metric.name(),
                    value,
                    today,
                    now
            );
        } else {
            learningProgressRepository.upsertDailyProgressAccumulate(
                    UUID.randomUUID(),
                    userId,
                    metric.name(),
                    value,
                    today,
                    now
            );
        }
    }

    @Transactional(readOnly = true)
    public ProgressHistoryDto getProgressHistory(UUID userId, MetricType metric, LocalDate from, LocalDate to) {
        List<LearningProgress> progressList = learningProgressRepository.findByUserAndMetricAndDateRange(userId, metric, from, to);

        // Build a map of date to value, filling in zeroes for dates with no entry
        Map<LocalDate, BigDecimal> dailyValues = new LinkedHashMap<>();
        LocalDate current = from;
        while (!current.isAfter(to)) {
            dailyValues.put(current, BigDecimal.ZERO);
            current = current.plusDays(1);
        }

        for (LearningProgress lp : progressList) {
            dailyValues.put(lp.getRecordedDate(), lp.getMetricValue());
        }

        return ProgressHistoryDto.builder()
                .metricType(metric)
                .from(from)
                .to(to)
                .dailyValues(dailyValues)
                .build();
    }

    public int calculateLevel(int totalXp) {
        // Thresholds: 0/100/300/600/1000/2000/5000
        if (totalXp < 100) return 1;
        if (totalXp < 300) return 2;
        if (totalXp < 600) return 3;
        if (totalXp < 1000) return 4;
        if (totalXp < 2000) return 5;
        if (totalXp < 5000) return 6;
        return 7;
    }

    public int calculateStreakDays(UUID userId) {
        List<java.sql.Date> sqlDates = studySessionRepository.findDistinctStudyDates(userId, DEFAULT_ZONE.getId());
        List<LocalDate> dates = sqlDates.stream()
                .map(java.sql.Date::toLocalDate)
                .collect(Collectors.toList());

        LocalDate today = LocalDate.now(DEFAULT_ZONE);
        LocalDate yesterday = today.minusDays(1);

        if (!dates.contains(today) && !dates.contains(yesterday)) {
            return 0;
        }

        LocalDate startDay = dates.contains(today) ? today : yesterday;
        int streak = 0;
        LocalDate current = startDay;
        while (dates.contains(current)) {
            streak++;
            current = current.minusDays(1);
        }
        return streak;
    }

    @Transactional(readOnly = true)
    public int getTotalXp(UUID userId) {
        BigDecimal totalXpVal = learningProgressRepository.sumMetricValueByUserIdAndMetricType(userId, MetricType.XP_TOTAL);
        return totalXpVal != null ? totalXpVal.intValue() : 0;
    }
}
