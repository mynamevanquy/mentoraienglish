package com.englishai.exercise.repository;

import com.englishai.exercise.entity.ExerciseAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExerciseAnswerRepository extends JpaRepository<ExerciseAnswer, UUID> {
    List<ExerciseAnswer> findByAttemptId(UUID attemptId);

    Optional<ExerciseAnswer> findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId);
}
