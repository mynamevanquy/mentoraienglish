package com.englishai.grammar.dto;

import com.englishai.common.enums.Level;

import java.util.List;
import java.util.UUID;

public record GrammarTopicDto(
        UUID id,
        String title,
        String descriptionVi,
        String descriptionEn,
        Level level,
        List<String> rules,
        List<String> examples,
        int orderIndex
) {
}
