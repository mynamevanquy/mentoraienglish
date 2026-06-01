package com.englishai.exercise.repository;

import com.englishai.exercise.entity.ExerciseQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExerciseQuestionRepository extends JpaRepository<ExerciseQuestion, UUID> {
    List<ExerciseQuestion> findByExerciseIdOrderByOrderIndexAsc(UUID exerciseId);
}
