package com.englishai.grammar.service;

import com.englishai.ai.dto.GrammarCorrectionResult;
import com.englishai.ai.dto.GrammarExplanationDto;
import com.englishai.ai.service.AiService;
import com.englishai.common.enums.Level;
import com.englishai.exercise.service.LearnerLevelService;
import com.englishai.grammar.dto.GrammarTopicDto;
import com.englishai.grammar.entity.GrammarTopic;
import com.englishai.grammar.repository.GrammarTopicRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrammarService {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final GrammarTopicRepository grammarTopicRepository;
    private final AiService aiService;
    private final ObjectMapper objectMapper;
    private final LearnerLevelService learnerLevelService;

    @Transactional(readOnly = true)
    public List<GrammarTopicDto> getPublishedTopics(Level level) {
        List<GrammarTopic> topics = level == null
                ? grammarTopicRepository.findByIsPublishedTrueOrderByOrderIndex()
                : grammarTopicRepository.findByLevelAndIsPublishedTrueOrderByOrderIndex(level);
        return topics.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public GrammarTopicDto getPublishedTopic(UUID id) {
        return grammarTopicRepository.findByIdAndIsPublishedTrue(id)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chủ điểm ngữ pháp."));
    }

    public GrammarCorrectionResult checkGrammar(UUID userId, String text) {
        if (text == null || text.isBlank()) {
            return new GrammarCorrectionResult("", "", Collections.emptyList(), false);
        }
        return aiService.correctGrammar(userId, text.trim()).join();
    }

    public GrammarExplanationDto explainWithAi(UUID userId, String topic) {
        String userLevel = learnerLevelService.determineLevel(userId).name();
        return aiService.explainGrammar(userId, topic, userLevel).join();
    }

    private GrammarTopicDto toDto(GrammarTopic topic) {
        return new GrammarTopicDto(
                topic.getId(),
                topic.getTitle(),
                topic.getDescriptionVi(),
                topic.getDescriptionEn(),
                topic.getLevel(),
                readStringList(topic.getRules()),
                readStringList(topic.getExamples()),
                topic.getOrderIndex()
        );
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (Exception e) {
            log.warn("Cannot parse grammar JSON content: {}", json, e);
            return Collections.emptyList();
        }
    }
}
