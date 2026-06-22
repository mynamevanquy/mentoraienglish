package com.englishai.exercise.repository;

import com.englishai.common.enums.AttemptStatus;
import com.englishai.dashboard.dto.WeakTopicDto;
import com.englishai.exercise.entity.ExerciseAttempt;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExerciseAttemptRepository extends JpaRepository<ExerciseAttempt, UUID> {

    @Query("SELECT ea FROM ExerciseAttempt ea WHERE ea.user.id = :userId ORDER BY ea.completedAt DESC")
    List<ExerciseAttempt> findRecentByUser(@Param("userId") UUID userId, Pageable pageable);

    Page<ExerciseAttempt> findByUserIdOrderByStartedAtDesc(UUID userId, Pageable pageable);

    Optional<ExerciseAttempt> findByIdAndUserId(UUID id, UUID userId);

    long countByUserIdAndStatusAndCompletedAtBetween(UUID userId, AttemptStatus status, Instant start, Instant end);

    @EntityGraph(attributePaths = "exercise")
    @Query("SELECT ea FROM ExerciseAttempt ea " +
           "WHERE ea.user.id = :userId AND ea.status = :status " +
           "AND ea.score IS NOT NULL AND ea.maxScore > 0 " +
           "AND NOT EXISTS (SELECT newer.id FROM ExerciseAttempt newer " +
           "WHERE newer.user.id = ea.user.id AND newer.exercise.id = ea.exercise.id " +
           "AND newer.status = :status AND newer.completedAt > ea.completedAt) " +
           "ORDER BY ea.completedAt DESC")
    List<ExerciseAttempt> findAssessmentEvidence(@Param("userId") UUID userId,
                                                 @Param("status") AttemptStatus status);

    @EntityGraph(attributePaths = {"exercise", "user"})
    @Query("SELECT ea FROM ExerciseAttempt ea " +
           "WHERE ea.user.id IN :userIds AND ea.status = :status " +
           "AND ea.score IS NOT NULL AND ea.maxScore > 0 " +
           "AND NOT EXISTS (SELECT newer.id FROM ExerciseAttempt newer " +
           "WHERE newer.user.id = ea.user.id AND newer.exercise.id = ea.exercise.id " +
           "AND newer.status = :status AND newer.completedAt > ea.completedAt) " +
           "ORDER BY ea.user.id, ea.completedAt DESC")
    List<ExerciseAttempt> findAssessmentEvidenceForUsers(@Param("userIds") Collection<UUID> userIds,
                                                         @Param("status") AttemptStatus status);

    @Query("SELECT COALESCE(AVG(ea.score * 100.0 / ea.maxScore), 0.0) FROM ExerciseAttempt ea " +
           "WHERE ea.user.id = :userId AND ea.status = :status AND ea.completedAt BETWEEN :start AND :end")
    double findAverageScorePercentage(@Param("userId") UUID userId,
                                      @Param("status") AttemptStatus status,
                                      @Param("start") Instant start,
                                      @Param("end") Instant end);

    @Query("SELECT new com.englishai.dashboard.dto.WeakTopicDto(gt.title, COALESCE(AVG(ea.score * 100.0 / ea.maxScore), 0.0), COUNT(ea.id)) " +
           "FROM ExerciseAttempt ea JOIN ea.exercise e JOIN e.relatedGrammar gt " +
           "WHERE ea.user.id = :userId AND ea.status = 'COMPLETED' AND gt.deletedAt IS NULL " +
           "GROUP BY gt.title " +
           "ORDER BY COALESCE(AVG(ea.score * 100.0 / ea.maxScore), 0.0) ASC")
    List<WeakTopicDto> findWeakGrammarTopics(@Param("userId") UUID userId, Pageable pageable);
}
