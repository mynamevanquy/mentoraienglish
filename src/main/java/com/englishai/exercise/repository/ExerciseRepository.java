package com.englishai.exercise.repository;

import com.englishai.exercise.entity.Exercise;
import com.englishai.common.enums.ExerciseType;
import com.englishai.common.enums.Level;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {
    List<Exercise> findByIsPublishedTrue();

    Page<Exercise> findByIsPublishedTrue(Pageable pageable);

    Page<Exercise> findByIsPublishedTrueAndTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Exercise> findByIsPublishedTrueAndExerciseType(ExerciseType exerciseType, Pageable pageable);

    Page<Exercise> findByIsPublishedTrueAndDifficultyLevel(Level difficultyLevel, Pageable pageable);

    Page<Exercise> findByIsPublishedTrueAndExerciseTypeAndDifficultyLevel(ExerciseType exerciseType, Level difficultyLevel, Pageable pageable);

    @EntityGraph(attributePaths = "questions")
    @Query("SELECT e FROM Exercise e WHERE e.isPublished = true AND (cast(:keyword as string) IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', cast(:keyword as string), '%'))) AND (:exerciseType IS NULL OR e.exerciseType = :exerciseType) AND (:difficulty IS NULL OR e.difficultyLevel = :difficulty)")
    Page<Exercise> searchPublished(@Param("keyword") String keyword,
                                   @Param("exerciseType") ExerciseType exerciseType,
                                   @Param("difficulty") Level difficulty,
                                   Pageable pageable);

    @EntityGraph(attributePaths = "questions")
    Optional<Exercise> findWithQuestionsById(UUID id);
}
