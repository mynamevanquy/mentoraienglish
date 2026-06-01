package com.englishai.exercise.dto;

import com.englishai.common.enums.ExerciseType;
import com.englishai.common.enums.Level;

public record ExerciseFilter(
        String keyword,
        ExerciseType exerciseType,
        Level difficulty
) {}
