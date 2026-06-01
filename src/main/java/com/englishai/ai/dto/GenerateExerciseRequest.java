package com.englishai.ai.dto;

/**
 * Request DTO specifying options for dynamic exercise generation.
 */
public record GenerateExerciseRequest(
    String topic,
    String exerciseType,
    String difficulty,
    int count
) {}
