package com.englishai.ai.service;

import com.englishai.ai.entity.AiPrompt;
import com.englishai.ai.repository.AiPromptRepository;
import com.englishai.ai.sanitizer.PromptSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromptBuilder {

    private final AiPromptRepository aiPromptRepository;
    private final PromptSanitizer promptSanitizer;

    private static final Map<String, String> FALLBACK_TEMPLATES = Map.of(
        "tutor_chat", "You are an expert AI English Tutor. Help the student learn English. Student level: {level}. Chat history: {history}. Student message: {message}",
        "exercise_generator", "Generate {count} English exercises about the topic: {topic}. Exercise type: {type}. Difficulty: {difficulty}. Return ONLY a JSON array matching the structure: [{\"questionText\": \"...\", \"options\": [\"Option A\", \"Option B\", ...], \"correctOption\": \"Correct Option\", \"explanation\": \"...\"}]",
        "grammar_explainer", "Explain the grammar topic '{topic}' in English and Vietnamese for a student at level {level}. Return ONLY a JSON object matching the structure: {\"topic\": \"...\", \"summary\": \"...\", \"rules\": [\"Rule 1\", ...], \"commonMistakes\": [\"Mistake 1\", ...], \"examples\": [\"Example 1\", ...]}",
        "vocabulary_explainer", "Explain the English word '{word}' in Vietnamese. In the JSON response, the 'definition' field must be a detailed explanation written in Vietnamese, the 'vietnameseMeaning' field must be a short Vietnamese meaning, and all entries in the 'examples' list must be English sentences followed by their Vietnamese translation in parentheses. Return ONLY a JSON object matching the structure: {\"word\": \"...\", \"ipa\": \"...\", \"partOfSpeech\": \"...\", \"vietnameseMeaning\": \"...\", \"definition\": \"...\", \"examples\": [\"Example 1 (Vietnamese translation)\", ...], \"synonyms\": [\"Synonym 1\", ...], \"antonyms\": [\"Antonym 1\", ...]}",
        "grammar_correction", "Check the grammar of the following text: \"{text}\". Return ONLY a JSON object matching the structure: {\"originalText\": \"...\", \"correctedText\": \"...\", \"hasErrors\": true/false, \"corrections\": [{\"originalSegment\": \"...\", \"correctedSegment\": \"...\", \"errorType\": \"...\", \"explanation\": \"...\"}]}"
    );

    @Cacheable(value = "prompts", key = "#name")
    public String getTemplate(String name) {
        return aiPromptRepository.findByName(name)
                .map(AiPrompt::getTemplate)
                .orElseGet(() -> {
                    log.warn("Prompt template '{}' not found in database. Using hardcoded fallback.", name);
                    return FALLBACK_TEMPLATES.getOrDefault(name, "");
                });
    }

    public String buildTutorPrompt(String conversationHistory, String userMessage, String userLevel) {
        String template = getTemplate("tutor_chat");
        String sanitizedHistory = promptSanitizer.sanitize(conversationHistory);
        String sanitizedMessage = promptSanitizer.sanitize(userMessage);

        return template
                .replace("{level}", userLevel != null ? userLevel : "BEGINNER")
                .replace("{history}", sanitizedHistory)
                .replace("{message}", sanitizedMessage);
    }

    public String buildExerciseGeneratorPrompt(String topic, String exerciseType, String difficulty, int count) {
        String template = getTemplate("exercise_generator");
        String sanitizedTopic = promptSanitizer.sanitize(topic);

        return template
                .replace("{topic}", sanitizedTopic)
                .replace("{type}", exerciseType)
                .replace("{difficulty}", difficulty)
                .replace("{count}", String.valueOf(count));
    }

    public String buildGrammarExplanationPrompt(String grammarTopic, String userLevel) {
        String template = getTemplate("grammar_explainer");
        String sanitizedTopic = promptSanitizer.sanitize(grammarTopic);

        return template
                .replace("{topic}", sanitizedTopic)
                .replace("{level}", userLevel != null ? userLevel : "BEGINNER")
                + "\nReturn a single valid JSON object only. Do not use Markdown, bullet points, code fences, or text outside JSON. "
                + "All string values must be valid JSON strings with escaped quotes/newlines when needed.";
    }

    public String buildVocabularyExplanationPrompt(String word) {
        String template = getTemplate("vocabulary_explainer");
        String sanitizedWord = promptSanitizer.sanitize(word);

        return template.replace("{word}", sanitizedWord);
    }

    public String buildGrammarCorrectionPrompt(String userText) {
        String template = getTemplate("grammar_correction");
        String sanitizedText = promptSanitizer.sanitize(userText);

        return template.replace("{text}", sanitizedText);
    }
}
