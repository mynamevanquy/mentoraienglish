package com.englishai.ai.dto;

import java.util.List;

/**
 * DTO representing detailed grammar topic explanations.
 */
public record GrammarExplanationDto(
    String topic,
    String summary,
    List<String> rules,
    List<String> commonMistakes,
    List<String> examples
) {}
