package com.englishai.ai.dto;

import java.util.List;

/**
 * Result representing the grammar check of a full paragraph of text.
 */
public record GrammarCorrectionResult(
    String originalText,
    String correctedText,
    List<GrammarCorrectionDto> corrections,
    boolean hasErrors
) {}
