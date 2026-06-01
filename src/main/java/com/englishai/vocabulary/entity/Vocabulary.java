package com.englishai.vocabulary.entity;

import com.englishai.common.entity.BaseEntity;
import com.englishai.common.enums.Level;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "vocabularies")
@SQLDelete(sql = "UPDATE vocabularies SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vocabulary extends BaseEntity {

    @Column(name = "word", nullable = false, length = 100)
    private String word;

    @Column(name = "phonetic", length = 100)
    private String phonetic;

    @Column(name = "part_of_speech", length = 50)
    private String partOfSpeech;

    @Column(name = "definition_en", nullable = false, columnDefinition = "TEXT")
    private String definitionEn;

    @Column(name = "definition_vi", columnDefinition = "TEXT")
    private String definitionVi;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "example_sentences", columnDefinition = "jsonb")
    private List<Map<String, Object>> exampleSentences;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", nullable = false, length = 20)
    private Level difficultyLevel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb")
    private List<String> tags;

    // New fields for synonyms and antonyms
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "synonyms", columnDefinition = "jsonb")
    private List<String> synonyms;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "antonyms", columnDefinition = "jsonb")
    private List<String> antonyms;

    // Wrapper getter methods for DTO compatibility
    public String getPronunciation() {
        return phonetic;
    }

    public List<Map<String, Object>> getExamples() {
        return exampleSentences;
    }

    public List<String> getSynonyms() {
        return synonyms;
    }

    public List<String> getAntonyms() {
        return antonyms;
    }
}
