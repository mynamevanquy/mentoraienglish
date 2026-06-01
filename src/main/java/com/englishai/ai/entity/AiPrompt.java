package com.englishai.ai.entity;

import com.englishai.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing an AI Prompt template stored in the database.
 * Used for dynamic prompts and cached with Spring Cache.
 */
@Entity
@Table(name = "ai_prompts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiPrompt extends BaseEntity {

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "template", nullable = false, columnDefinition = "TEXT")
    private String template;
}
