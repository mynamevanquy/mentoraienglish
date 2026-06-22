package com.englishai.vocabulary.repository;

import com.englishai.common.enums.Level;
import com.englishai.vocabulary.entity.UserVocabulary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserVocabularyRepository extends JpaRepository<UserVocabulary, UUID> {

    @Query("SELECT uv FROM UserVocabulary uv JOIN FETCH uv.vocabulary WHERE uv.user.id = :userId AND uv.nextReviewAt <= :now")
    List<UserVocabulary> findDueForReview(@Param("userId") UUID userId, @Param("now") Instant now);

    @Query("SELECT uv FROM UserVocabulary uv JOIN FETCH uv.vocabulary WHERE uv.user.id = :userId AND uv.nextReviewAt <= :now ORDER BY uv.nextReviewAt ASC")
    List<UserVocabulary> findDueForReview(@Param("userId") UUID userId, @Param("now") Instant now, Pageable pageable);

    @EntityGraph(attributePaths = "vocabulary")
    Page<UserVocabulary> findByUserId(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = "vocabulary")
    Optional<UserVocabulary> findByUserIdAndVocabularyId(UUID userId, UUID vocabularyId);

    @Query("SELECT uv.masteryLevel, COUNT(uv) FROM UserVocabulary uv WHERE uv.user.id = :userId GROUP BY uv.masteryLevel")
    List<Object[]> countByMasteryLevel(@Param("userId") UUID userId);

    long countByUserIdAndMasteryLevelGreaterThanEqual(UUID userId, int masteryLevel);

    long countByUserIdAndNextReviewAtLessThanEqual(UUID userId, Instant now);

    long countByUserIdAndCreatedAtBetween(UUID userId, Instant from, Instant to);

    @Query("SELECT uv FROM UserVocabulary uv JOIN FETCH uv.vocabulary v " +
           "WHERE uv.user.id = :userId " +
           "AND (:level IS NULL OR v.difficultyLevel = :level) " +
           "AND (:excludedVocabularyId IS NULL OR v.id <> :excludedVocabularyId) " +
           "ORDER BY FUNCTION('RANDOM')")
    List<UserVocabulary> findRandomByUserExcluding(
            @Param("userId") UUID userId,
            @Param("level") Level level,
            @Param("excludedVocabularyId") UUID excludedVocabularyId,
            Pageable pageable);

    long countByUserId(UUID userId);

    @Query("SELECT COALESCE(SUM(uv.reviewCount), 0) FROM UserVocabulary uv WHERE uv.user.id = :userId")
    long sumReviewCountByUserId(@Param("userId") UUID userId);

    @Query(value = "SELECT CAST(uv.created_at AT TIME ZONE :tz AS DATE) AS added_date, COUNT(*) AS cnt " +
                   "FROM user_vocabularies uv WHERE uv.user_id = :userId " +
                   "GROUP BY added_date ORDER BY added_date",
           nativeQuery = true)
    List<Object[]> countWordsAddedByDate(@Param("userId") UUID userId, @Param("tz") String timezone);
}
