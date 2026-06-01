package com.englishai.vocabulary.dto;

import com.englishai.vocabulary.entity.Vocabulary;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * Simple projection of Vocabulary entity for list/detail views.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyDto {
    private String id;
    private String word;
    private String pronunciation;
    private String partOfSpeech;
    private String definitionEn;
    private String definitionVi;
    private List<Map<String, Object>> examples;
    private List<String> synonyms;
    private List<String> antonyms;
    private String difficultyLevel;

    public static VocabularyDto from(Vocabulary v) {
        VocabularyDto dto = new VocabularyDto();
        dto.setId(v.getId().toString());
        dto.setWord(v.getWord());
        dto.setPronunciation(v.getPronunciation());
        dto.setPartOfSpeech(v.getPartOfSpeech());
        dto.setDefinitionEn(v.getDefinitionEn());
        dto.setDefinitionVi(v.getDefinitionVi());
        // Ensure collections are never null to avoid Thymeleaf iteration errors
        dto.setExamples(v.getExamples() != null ? v.getExamples() : java.util.Collections.emptyList());
        dto.setSynonyms(v.getSynonyms() != null ? v.getSynonyms() : java.util.Collections.emptyList());
        dto.setAntonyms(v.getAntonyms() != null ? v.getAntonyms() : java.util.Collections.emptyList());
        dto.setDifficultyLevel(v.getDifficultyLevel() != null ? v.getDifficultyLevel().name() : null);
        return dto;
    }
}
