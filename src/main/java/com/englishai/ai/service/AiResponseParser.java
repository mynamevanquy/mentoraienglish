package com.englishai.ai.service;

import com.englishai.ai.dto.ExerciseQuestionDto;
import com.englishai.ai.dto.GrammarCorrectionResult;
import com.englishai.ai.dto.GrammarExplanationDto;
import com.englishai.ai.dto.VocabularyInfoDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiResponseParser {

    private final ObjectMapper objectMapper;

    /**
     * Cleans markdown JSON wrappers (e.g. ```json ... ```) from LLM output.
     */
    private String cleanJson(String content) {
        if (content == null) {
            return "";
        }
        content = content.trim();
        if (content.startsWith("```")) {
            content = content.replaceAll("^```[a-zA-Z]*\\s*", "");
            content = content.replaceAll("\\s*```$", "");
        }
        return content.trim();
    }

    public List<ExerciseQuestionDto> parseExerciseList(String content) {
        String cleaned = cleanJson(content);
        try {
            // Try direct array first
            return objectMapper.readValue(cleaned, new TypeReference<List<ExerciseQuestionDto>>() {});
        } catch (Exception e1) {
            try {
                // AI may wrap array in an object like {"exercises": [...]}
                var node = objectMapper.readTree(cleaned);
                if (node.isArray()) {
                    return objectMapper.convertValue(node, new TypeReference<List<ExerciseQuestionDto>>() {});
                }
                // Find first array field in the object
                var fieldNames = node.fieldNames();
                while (fieldNames.hasNext()) {
                    var field = node.get(fieldNames.next());
                    if (field.isArray() && field.size() > 0) {
                        return objectMapper.convertValue(field, new TypeReference<List<ExerciseQuestionDto>>() {});
                    }
                }
                log.error("No array found in AI exercise output. Content: {}", content);
                return Collections.emptyList();
            } catch (Exception e2) {
                log.error("Failed to parse exercises from AI output. Content: {}", content, e2);
                return Collections.emptyList();
            }
        }
    }

    public GrammarCorrectionResult parseGrammarCorrections(String content) {
        String cleaned = cleanJson(content);
        try {
            return objectMapper.readValue(cleaned, GrammarCorrectionResult.class);
        } catch (Exception e) {
            log.error("Failed to parse grammar corrections from AI output. Content: {}", content, e);
            // Return fallback without corrections
            return new GrammarCorrectionResult(content, content, Collections.emptyList(), false);
        }
    }

    public VocabularyInfoDto parseVocabularyInfo(String content) {
        String cleaned = cleanJson(content);
        try {
            return objectMapper.readValue(cleaned, VocabularyInfoDto.class);
        } catch (Exception e) {
            log.error("Failed to parse vocabulary info from AI output. Content: {}", content, e);
            return new VocabularyInfoDto(content, "", "", "", "", Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }
    }

    public GrammarExplanationDto parseGrammarExplanation(String content) {
        String cleaned = cleanJson(content);
        try {
            return objectMapper.readValue(cleaned, GrammarExplanationDto.class);
        } catch (Exception e) {
            log.error("Failed to parse grammar explanation from AI output. Content: {}", content, e);
            return new GrammarExplanationDto(content, "", Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }
    }
}
