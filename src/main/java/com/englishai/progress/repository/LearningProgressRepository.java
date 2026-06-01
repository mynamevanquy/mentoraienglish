package com.englishai.progress.repository;

import com.englishai.progress.entity.LearningProgress;
import com.englishai.common.enums.MetricType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LearningProgressRepository extends JpaRepository<LearningProgress, UUID> {

    @Query("SELECT lp FROM LearningProgress lp WHERE lp.user.id = :userId AND lp.recordedDate BETWEEN :from AND :to ORDER BY lp.recordedDate ASC")
    List<LearningProgress> findByUserAndDateRange(
            @Param("userId") UUID userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query("SELECT lp FROM LearningProgress lp WHERE lp.user.id = :userId AND lp.metricType = :metricType AND lp.recordedDate BETWEEN :from AND :to ORDER BY lp.recordedDate ASC")
    List<LearningProgress> findByUserAndMetricAndDateRange(
            @Param("userId") UUID userId,
            @Param("metricType") MetricType metricType,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query("SELECT COALESCE(SUM(lp.metricValue), 0) FROM LearningProgress lp WHERE lp.user.id = :userId AND lp.metricType = :metricType")
    BigDecimal sumMetricValueByUserIdAndMetricType(@Param("userId") UUID userId, @Param("metricType") MetricType metricType);

    Optional<LearningProgress> findByUserIdAndMetricTypeAndRecordedDate(UUID userId, MetricType metricType, LocalDate recordedDate);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO learning_progress (id, user_id, metric_type, metric_value, recorded_date, updated_at) " +
                   "VALUES (:id, :userId, :metricType, :metricValue, :recordedDate, :updatedAt) " +
                   "ON CONFLICT (user_id, metric_type, recorded_date) " +
                   "DO UPDATE SET metric_value = learning_progress.metric_value + EXCLUDED.metric_value, updated_at = EXCLUDED.updated_at",
           nativeQuery = true)
    void upsertDailyProgressAccumulate(@Param("id") UUID id,
                                       @Param("userId") UUID userId,
                                       @Param("metricType") String metricType,
                                       @Param("metricValue") BigDecimal metricValue,
                                       @Param("recordedDate") LocalDate recordedDate,
                                       @Param("updatedAt") Instant updatedAt);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO learning_progress (id, user_id, metric_type, metric_value, recorded_date, updated_at) " +
                   "VALUES (:id, :userId, :metricType, :metricValue, :recordedDate, :updatedAt) " +
                   "ON CONFLICT (user_id, metric_type, recorded_date) " +
                   "DO UPDATE SET metric_value = EXCLUDED.metric_value, updated_at = EXCLUDED.updated_at",
           nativeQuery = true)
    void upsertDailyProgressOverwrite(@Param("id") UUID id,
                                      @Param("userId") UUID userId,
                                      @Param("metricType") String metricType,
                                      @Param("metricValue") BigDecimal metricValue,
                                      @Param("recordedDate") LocalDate recordedDate,
                                      @Param("updatedAt") Instant updatedAt);
}
