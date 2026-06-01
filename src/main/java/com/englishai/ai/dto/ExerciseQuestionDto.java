package com.englishai.ai.dto;

import java.util.List;

/**
 * DTO representing an generated exercise question.
 */
public record ExerciseQuestionDto(
    String questionText,
    List<String> options,
    String correctOption,
    String explanation
) {}
