package com.englishai.progress.repository;

import com.englishai.dashboard.dto.DashboardSummaryProjection;
import com.englishai.progress.entity.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface StudySessionRepository extends JpaRepository<StudySession, UUID> {

    List<StudySession> findByUserIdAndStartedAtBetween(UUID userId, Instant start, Instant end);

    /** Sum of study minutes for a user between two instants (used for "today" calculation). */
    @Query("SELECT COALESCE(SUM(s.durationMinutes), 0) FROM StudySession s " +
           "WHERE s.user.id = :userId AND s.startedAt BETWEEN :start AND :end")
    int sumMinutesBetween(@Param("userId") UUID userId,
                          @Param("start") Instant start,
                          @Param("end") Instant end);

    /** Get distinct dates (in a timezone) that have study sessions, ordered descending — for streak calc. */
    @Query(value = "SELECT DISTINCT CAST(s.started_at AT TIME ZONE :tz AS DATE) AS study_date " +
                   "FROM study_sessions s WHERE s.user_id = :userId " +
                   "ORDER BY study_date DESC",
           nativeQuery = true)
    List<java.sql.Date> findDistinctStudyDates(@Param("userId") UUID userId,
                                               @Param("tz") String timezone);

    /** Study minutes grouped by day for a date range — for weekly bar chart. */
    @Query(value = "SELECT CAST(s.started_at AT TIME ZONE :tz AS DATE) AS study_date, " +
                   "COALESCE(SUM(s.duration_minutes), 0) AS total_minutes " +
                   "FROM study_sessions s " +
                   "WHERE s.user_id = :userId " +
                   "  AND s.started_at BETWEEN :start AND :end " +
                   "GROUP BY study_date ORDER BY study_date",
           nativeQuery = true)
    List<Object[]> findDailyMinutes(@Param("userId") UUID userId,
                                    @Param("start") Instant start,
                                    @Param("end") Instant end,
                                    @Param("tz") String timezone);

    /** Recent study sessions ordered by start time descending for activity feed. */
    @Query("SELECT s FROM StudySession s WHERE s.user.id = :userId ORDER BY s.startedAt DESC")
    List<StudySession> findRecentByUser(@Param("userId") UUID userId, org.springframework.data.domain.Pageable pageable);

    /** Count vocabulary review sessions grouped by day for a date range. */
    @Query(value = "SELECT CAST(s.started_at AT TIME ZONE :tz AS DATE) AS review_date, COUNT(*) AS cnt " +
                   "FROM study_sessions s " +
                   "WHERE s.user_id = :userId AND s.activity_type = 'VOCABULARY' " +
                   "  AND s.started_at BETWEEN :start AND :end " +
                   "GROUP BY review_date ORDER BY review_date",
           nativeQuery = true)
    List<Object[]> findDailyVocabularyReviews(@Param("userId") UUID userId,
                                              @Param("start") Instant start,
                                              @Param("end") Instant end,
                                              @Param("tz") String timezone);

    @Query(value = """
            WITH study_dates AS (
                SELECT DISTINCT CAST(s.started_at AT TIME ZONE :timezone AS DATE) AS study_date
                FROM study_sessions s
                WHERE s.user_id = :userId
            ),
            anchor AS (
                SELECT CASE
                    WHEN EXISTS (SELECT 1 FROM study_dates WHERE study_date = CAST(:today AS DATE)) THEN CAST(:today AS DATE)
                    WHEN EXISTS (SELECT 1 FROM study_dates WHERE study_date = CAST(:yesterday AS DATE)) THEN CAST(:yesterday AS DATE)
                    ELSE NULL
                END AS anchor_date
            ),
            ordered_dates AS (
                SELECT sd.study_date,
                       a.anchor_date,
                       ROW_NUMBER() OVER (ORDER BY sd.study_date DESC) AS position
                FROM study_dates sd
                CROSS JOIN anchor a
                WHERE a.anchor_date IS NOT NULL
                  AND sd.study_date <= a.anchor_date
            ),
            streak AS (
                SELECT CASE
                    WHEN (SELECT anchor_date FROM anchor) IS NULL THEN 0
                    ELSE COALESCE(
                        (SELECT MIN(position) - 1
                         FROM ordered_dates
                         WHERE study_date <> CAST(anchor_date - ((position - 1) * INTERVAL '1 day') AS DATE)),
                        (SELECT COUNT(*) FROM ordered_dates)
                    )
                END AS current_streak_days
            )
            SELECT
                (SELECT COALESCE(SUM(s.duration_minutes), 0)
                 FROM study_sessions s
                 WHERE s.user_id = :userId AND s.started_at >= :startOfDay AND s.started_at < :endOfDay) AS "todayStudyMinutes",
                (SELECT current_streak_days FROM streak) AS "currentStreakDays",
                (SELECT COUNT(*)
                 FROM user_vocabularies uv
                 WHERE uv.user_id = :userId AND uv.mastery_level >= 4) AS "vocabularyMastered",
                (SELECT COUNT(*)
                 FROM user_vocabularies uv
                 WHERE uv.user_id = :userId AND uv.next_review_at <= :now) AS "vocabularyDueReview",
                (SELECT COUNT(*)
                 FROM exercise_attempts ea
                 WHERE ea.user_id = :userId AND ea.status = 'COMPLETED' AND ea.completed_at >= :startOfWeek AND ea.completed_at <= :now) AS "exercisesCompletedThisWeek",
                (SELECT COALESCE(AVG(ea.score * 100.0 / NULLIF(ea.max_score, 0)), 0)
                 FROM exercise_attempts ea
                 WHERE ea.user_id = :userId AND ea.status = 'COMPLETED' AND ea.completed_at >= :startOfWeek AND ea.completed_at <= :now) AS "averageExerciseScore",
                (SELECT COALESCE(SUM(lp.metric_value), 0)
                 FROM learning_progress lp
                 WHERE lp.user_id = :userId AND lp.metric_type = 'XP_TOTAL') AS "totalXp",
                (SELECT COUNT(*)
                 FROM conversations c
                 WHERE c.user_id = :userId AND c.status = 'ACTIVE' AND c.deleted_at IS NULL) AS "activeConversations"
            """, nativeQuery = true)
    DashboardSummaryProjection findDashboardSummary(
            @Param("userId") UUID userId,
            @Param("timezone") String timezone,
            @Param("today") LocalDate today,
            @Param("yesterday") LocalDate yesterday,
            @Param("startOfDay") Instant startOfDay,
            @Param("endOfDay") Instant endOfDay,
            @Param("startOfWeek") Instant startOfWeek,
            @Param("now") Instant now);
}
