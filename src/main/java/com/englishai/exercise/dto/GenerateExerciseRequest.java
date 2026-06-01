package com.englishai.exercise.dto;

import com.englishai.common.enums.ExerciseType;
import com.englishai.common.enums.Level;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateExerciseRequest {
    @NotBlank
    private String topic;

    @NotNull
    private ExerciseType exerciseType;

    @NotNull
    private Level difficulty;

    @Min(1)
    @Max(20)
    private int questionCount = 5;

    private UUID relatedLessonId;
}
