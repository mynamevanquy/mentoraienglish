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
import java.util.Optional;

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

    private String extractJsonValue(String content) {
        String cleaned = cleanJson(content);
        if (cleaned.isEmpty()) {
            return cleaned;
        }

        Optional<String> object = extractBalancedJson(cleaned, '{', '}');
        Optional<String> array = extractBalancedJson(cleaned, '[', ']');

        if (object.isPresent() && array.isPresent()) {
            return cleaned.indexOf('{') < cleaned.indexOf('[') ? object.get() : array.get();
        }
        return object.or(() -> array).orElse(cleaned);
    }

    private boolean isUnbalancedJsonObject(String content) {
        return content.startsWith("{") && extractBalancedJson(content, '{', '}').isEmpty();
    }

    private Optional<String> extractBalancedJson(String content, char opening, char closing) {
        int start = content.indexOf(opening);
        if (start < 0) {
            return Optional.empty();
        }

        boolean insideString = false;
        boolean escaped = false;
        int depth = 0;

        for (int i = start; i < content.length(); i++) {
            char current = content.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\' && insideString) {
                escaped = true;
                continue;
            }
            if (current == '"') {
                insideString = !insideString;
                continue;
            }
            if (insideString) {
                continue;
            }
            if (current == opening) {
                depth++;
                continue;
            }
            if (current == closing) {
                depth--;
                if (depth == 0) {
                    return Optional.of(content.substring(start, i + 1));
                }
            }
        }

        return Optional.empty();
    }

    public List<ExerciseQuestionDto> parseExerciseList(String content) {
        String cleaned = extractJsonValue(content);
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
        String cleaned = extractJsonValue(content);
        if (!cleaned.startsWith("{")) {
            log.warn("AI grammar correction output did not contain a JSON object.");
            return new GrammarCorrectionResult(content, content, Collections.emptyList(), false);
        }
        if (isUnbalancedJsonObject(cleaned)) {
            log.warn("AI grammar correction output was truncated before the JSON object ended.");
            return new GrammarCorrectionResult(content, content, Collections.emptyList(), false);
        }
        try {
            return objectMapper.readValue(cleaned, GrammarCorrectionResult.class);
        } catch (Exception e) {
            log.error("Failed to parse grammar corrections from AI output. Content: {}", content, e);
            // Return fallback without corrections
            return new GrammarCorrectionResult(content, content, Collections.emptyList(), false);
        }
    }

    public VocabularyInfoDto parseVocabularyInfo(String content) {
        String cleaned = extractJsonValue(content);
        if (!cleaned.startsWith("{")) {
            log.warn("AI vocabulary output did not contain a JSON object.");
            return new VocabularyInfoDto(content, "", "", "", "", Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }
        if (isUnbalancedJsonObject(cleaned)) {
            log.warn("AI vocabulary output was truncated before the JSON object ended.");
            return new VocabularyInfoDto(content, "", "", "", "", Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }
        try {
            return objectMapper.readValue(cleaned, VocabularyInfoDto.class);
        } catch (Exception e) {
            log.error("Failed to parse vocabulary info from AI output. Content: {}", content, e);
            return new VocabularyInfoDto(content, "", "", "", "", Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }
    }

    public GrammarExplanationDto parseGrammarExplanation(String content) {
        String cleaned = extractJsonValue(content);
        if (!cleaned.startsWith("{")) {
            log.warn("AI grammar explanation output did not contain a JSON object.");
            return new GrammarExplanationDto(content, "", Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }
        if (isUnbalancedJsonObject(cleaned)) {
            log.warn("AI grammar explanation output was truncated before the JSON object ended.");
            return new GrammarExplanationDto(content, "", Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }
        try {
            return objectMapper.readValue(cleaned, GrammarExplanationDto.class);
        } catch (Exception e) {
            log.error("Failed to parse grammar explanation from AI output. Content: {}", content, e);
            return new GrammarExplanationDto(content, "", Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }
    }
}
