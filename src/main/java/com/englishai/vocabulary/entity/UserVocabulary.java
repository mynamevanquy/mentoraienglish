package com.englishai.vocabulary.entity;

import com.englishai.common.entity.BaseEntity;
import com.englishai.user.entity.User;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "user_vocabularies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserVocabulary extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vocabulary_id", nullable = false)
    private Vocabulary vocabulary;

    @Column(name = "mastery_level", nullable = false)
    @Builder.Default
    private int masteryLevel = 0;

    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private int reviewCount = 0;

    @Column(name = "next_review_at", nullable = false)
    private Instant nextReviewAt;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    @Column(name = "sm2_easiness_factor", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal sm2EasinessFactor = new BigDecimal("2.50");

    @Column(name = "sm2_interval", nullable = false)
    @Builder.Default
    private int sm2Interval = 0;

    @Column(name = "sm2_repetitions", nullable = false)
    @Builder.Default
    private int sm2Repetitions = 0;

    @Column(name = "is_bookmarked", nullable = false)
    @Builder.Default
    private boolean isBookmarked = false;

    /**
     * Cached AI explanation stored as JSONB.
     * Keys: word, ipa, partOfSpeech, vietnameseMeaning, definition, examples, synonyms, antonyms
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;
}
