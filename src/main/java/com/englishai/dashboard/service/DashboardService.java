package com.englishai.dashboard.service;

import com.englishai.common.enums.ActivityType;
import com.englishai.common.enums.AttemptStatus;
import com.englishai.common.enums.ConversationStatus;
import com.englishai.common.enums.MetricType;
import com.englishai.conversation.repository.ConversationRepository;
import com.englishai.dashboard.dto.DashboardSummaryProjection;
import com.englishai.dashboard.dto.DashboardSummaryDto;
import com.englishai.dashboard.dto.RecentActivityDto;
import com.englishai.dashboard.dto.WeakTopicDto;
import com.englishai.dashboard.dto.WeeklyProgressDto;
import com.englishai.exercise.entity.ExerciseAttempt;
import com.englishai.exercise.repository.ExerciseAttemptRepository;
import com.englishai.progress.entity.StudySession;
import com.englishai.progress.repository.LearningProgressRepository;
import com.englishai.progress.repository.StudySessionRepository;
import com.englishai.progress.service.ProgressService;
import com.englishai.vocabulary.repository.UserVocabularyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final StudySessionRepository studySessionRepository;
    private final LearningProgressRepository learningProgressRepository;
    private final UserVocabularyRepository userVocabularyRepository;
    private final ExerciseAttemptRepository exerciseAttemptRepository;
    private final ConversationRepository conversationRepository;
    private final ProgressService progressService;

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Transactional(readOnly = true)
    public DashboardSummaryDto getSummary(UUID userId) {
        LocalDate today = LocalDate.now(DEFAULT_ZONE);
        Instant startOfDay = today.atStartOfDay(DEFAULT_ZONE).toInstant();
        Instant endOfDay = today.plusDays(1).atStartOfDay(DEFAULT_ZONE).toInstant();
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Instant startOfWeekInstant = startOfWeek.atStartOfDay(DEFAULT_ZONE).toInstant();
        Instant now = Instant.now();
        DashboardSummaryProjection summary = studySessionRepository.findDashboardSummary(
                userId,
                DEFAULT_ZONE.getId(),
                today,
                today.minusDays(1),
                startOfDay,
                endOfDay,
                startOfWeekInstant,
                now);

        return DashboardSummaryDto.builder()
                .userId(userId)
                .todayStudyMinutes(intValue(summary.getTodayStudyMinutes()))
                .currentStreakDays(intValue(summary.getCurrentStreakDays()))
                .vocabularyMastered(longValue(summary.getVocabularyMastered()))
                .vocabularyDueReview(longValue(summary.getVocabularyDueReview()))
                .exercisesCompletedThisWeek(longValue(summary.getExercisesCompletedThisWeek()))
                .averageExerciseScore(doubleValue(summary.getAverageExerciseScore()))
                .totalXp(longValue(summary.getTotalXp()))
                .activeConversations(longValue(summary.getActiveConversations()))
                .build();
    }

    private int intValue(Number value) {
        return value != null ? value.intValue() : 0;
    }

    private long longValue(Number value) {
        return value != null ? value.longValue() : 0L;
    }

    private double doubleValue(Number value) {
        return value != null ? value.doubleValue() : 0.0;
    }

    @Transactional(readOnly = true)
    public List<RecentActivityDto> getRecentActivity(UUID userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);

        // Fetch study sessions and exercise attempts
        List<StudySession> studySessions = studySessionRepository.findRecentByUser(userId, pageable);
        List<ExerciseAttempt> exerciseAttempts = exerciseAttemptRepository.findRecentByUser(userId, pageable);

        List<RecentActivityDto> activityFeed = new ArrayList<>();

        // Map study sessions
        for (StudySession session : studySessions) {
            activityFeed.add(RecentActivityDto.builder()
                    .activityType(session.getActivityType())
                    .activityId(session.getActivityId() != null ? session.getActivityId() : session.getId())
                    .startedAt(session.getStartedAt())
                    .durationMinutes(session.getDurationMinutes())
                    .xpEarned(session.getXpEarned())
                    .build());
        }

        // Map exercise attempts
        for (ExerciseAttempt attempt : exerciseAttempts) {
            int xp = 0;
            if (attempt.getScore() != null && attempt.getMaxScore() != null && attempt.getMaxScore().doubleValue() > 0) {
                xp = (int) Math.round(5.0 * (attempt.getScore().doubleValue() / attempt.getMaxScore().doubleValue()) * 10.0);
            }
            activityFeed.add(RecentActivityDto.builder()
                    .activityType(ActivityType.EXERCISE)
                    .activityId(attempt.getId())
                    .startedAt(attempt.getCompletedAt() != null ? attempt.getCompletedAt() : attempt.getStartedAt())
                    .durationMinutes(attempt.getTimeSpentSeconds() != null ? attempt.getTimeSpentSeconds() / 60 : 0)
                    .xpEarned(xp)
                    .build());
        }

        // Sort descending by timestamp (startedAt) and limit
        return activityFeed.stream()
                .sorted(Comparator.comparing(RecentActivityDto::getStartedAt).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WeeklyProgressDto getWeeklyProgress(UUID userId) {
        LocalDate today = LocalDate.now(DEFAULT_ZONE);
        LocalDate sevenDaysAgo = today.minusDays(6);
        Instant start = sevenDaysAgo.atStartOfDay(DEFAULT_ZONE).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(DEFAULT_ZONE).toInstant();

        List<Object[]> results = studySessionRepository.findDailyMinutes(userId, start, end, DEFAULT_ZONE.getId());

        Map<LocalDate, Integer> dailyMinutesMap = new LinkedHashMap<>();
        LocalDate current = sevenDaysAgo;
        while (!current.isAfter(today)) {
            dailyMinutesMap.put(current, 0);
            current = current.plusDays(1);
        }

        for (Object[] row : results) {
            if (row[0] != null) {
                LocalDate date;
                if (row[0] instanceof java.sql.Date) {
                    date = ((java.sql.Date) row[0]).toLocalDate();
                } else if (row[0] instanceof LocalDate) {
                    date = (LocalDate) row[0];
                } else {
                    date = LocalDate.parse(row[0].toString());
                }
                Number minutes = (Number) row[1];
                dailyMinutesMap.put(date, minutes != null ? minutes.intValue() : 0);
            }
        }

        return WeeklyProgressDto.builder()
                .minutesPerDay(dailyMinutesMap)
                .build();
    }

    @Transactional(readOnly = true)
    public List<WeakTopicDto> getWeakGrammarTopics(UUID userId, int limit) {
        return exerciseAttemptRepository.findWeakGrammarTopics(userId, PageRequest.of(0, limit));
    }
}
