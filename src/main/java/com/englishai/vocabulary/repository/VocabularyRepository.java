package com.englishai.vocabulary.repository;

import com.englishai.common.enums.Level;
import com.englishai.vocabulary.entity.Vocabulary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VocabularyRepository extends JpaRepository<Vocabulary, UUID> {
    Optional<Vocabulary> findByWord(String word);

    Page<Vocabulary> findByWordContainingIgnoreCase(String keyword, Pageable pageable);

    Page<Vocabulary> findByWordContainingIgnoreCaseAndDifficultyLevel(String keyword, Level difficultyLevel, Pageable pageable);

    Page<Vocabulary> findByDifficultyLevel(Level difficultyLevel, Pageable pageable);

    long countByDifficultyLevel(Level difficultyLevel);

    @Query("SELECT v FROM Vocabulary v WHERE v.id NOT IN " +
           "(SELECT uv.vocabulary.id FROM UserVocabulary uv WHERE uv.user.id = :userId) " +
           "AND (:level IS NULL OR v.difficultyLevel = :level) " +
           "ORDER BY FUNCTION('RANDOM')")
    List<Vocabulary> findUnlearnedByUser(@Param("userId") UUID userId,
                                         @Param("level") Level level,
                                         Pageable pageable);
}
