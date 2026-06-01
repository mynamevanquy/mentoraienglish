package com.englishai.ai.dto;

import java.util.List;

/**
 * DTO representing detailed vocabulary explanation.
 */
public record VocabularyInfoDto(
    String word,
    String ipa,
    String partOfSpeech,
    String vietnameseMeaning,
    String definition,
    List<String> examples,
    List<String> synonyms,
    List<String> antonyms
) {}
