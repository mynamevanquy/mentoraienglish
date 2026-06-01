package com.englishai.ai.dto;

/**
 * DTO representing a specific grammatical error correction.
 */
public record GrammarCorrectionDto(
    String originalSegment,
    String correctedSegment,
    String errorType,
    String explanation
) {}
