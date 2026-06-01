package com.englishai.vocabulary.dto;

import com.englishai.vocabulary.entity.UserVocabulary;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.List;

/**
 * DTO for the UserVocabulary entity that includes SM‑2 fields and optional AI metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVocabularyDto {
    private String id;
    private String vocabularyId;
    private String word;
    private String pronunciation;
    private String partOfSpeech;
    private String definitionEn;
    private String definitionVi;
    private List<Map<String, Object>> examples;
    private List<String> synonyms;
    private List<String> antonyms;

    private int masteryLevel;
    private int reviewCount;
    private int sm2Interval;
    private int sm2Repetitions;
    private double sm2EasinessFactor;
    private Instant nextReviewAt;
    private Instant lastReviewedAt;
    private boolean bookmarked;

    // Cached AI explanation (may be null)
    private Map<String, Object> metadata;

    public static UserVocabularyDto from(UserVocabulary uv) {
        UserVocabularyDto dto = new UserVocabularyDto();
        dto.setId(uv.getId().toString());
        if (uv.getVocabulary() != null) {
            dto.setVocabularyId(uv.getVocabulary().getId().toString());
            dto.setWord(uv.getVocabulary().getWord());
            dto.setPronunciation(uv.getVocabulary().getPronunciation());
            dto.setPartOfSpeech(uv.getVocabulary().getPartOfSpeech());
            dto.setDefinitionEn(uv.getVocabulary().getDefinitionEn());
            dto.setDefinitionVi(uv.getVocabulary().getDefinitionVi());
            dto.setExamples(uv.getVocabulary().getExamples());
            dto.setSynonyms(uv.getVocabulary().getSynonyms());
            dto.setAntonyms(uv.getVocabulary().getAntonyms());
        }
        dto.setMasteryLevel(uv.getMasteryLevel());
        dto.setReviewCount(uv.getReviewCount());
        dto.setSm2Interval(uv.getSm2Interval());
        dto.setSm2Repetitions(uv.getSm2Repetitions());
        dto.setSm2EasinessFactor(uv.getSm2EasinessFactor().doubleValue());
        dto.setNextReviewAt(uv.getNextReviewAt());
        dto.setLastReviewedAt(uv.getLastReviewedAt());
        dto.setBookmarked(uv.isBookmarked());
        dto.setMetadata(uv.getMetadata());
        return dto;
    }
}
