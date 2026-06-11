package com.englishai.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiResponseParserTest {

    private final AiResponseParser parser = new AiResponseParser(new ObjectMapper());

    @Test
    void parsesGrammarExplanationFromMarkdownJsonBlock() {
        String content = """
                ```json
                {
                  "topic": "Passive gerund",
                  "summary": "Use a passive gerund after some prepositions.",
                  "rules": ["Use being + past participle."],
                  "commonMistakes": ["Use base verb after preposition."],
                  "examples": ["He objects to being treated unfairly."]
                }
                ```
                """;

        var result = parser.parseGrammarExplanation(content);

        assertThat(result.topic()).isEqualTo("Passive gerund");
        assertThat(result.rules()).containsExactly("Use being + past participle.");
        assertThat(result.examples()).containsExactly("He objects to being treated unfairly.");
    }

    @Test
    void parsesGrammarExplanationWhenAiAddsTextAroundJson() {
        String content = """
                Here is the JSON:
                {
                  "topic": "Passive gerund",
                  "summary": "Use a passive gerund after some verbs and prepositions.",
                  "rules": ["After prepositions, use being + past participle."],
                  "commonMistakes": ["Do not use to be + past participle after a preposition."],
                  "examples": ["He strongly objects to being treated as a subordinate."]
                }
                Hope this helps.
                """;

        var result = parser.parseGrammarExplanation(content);

        assertThat(result.topic()).isEqualTo("Passive gerund");
        assertThat(result.commonMistakes())
                .containsExactly("Do not use to be + past participle after a preposition.");
    }

    @Test
    void returnsFallbackForUnbalancedPartialGrammarExplanation() {
        String content = """
                ) "
                * "Example 2: 'He strongly objects to being treated as a subordinate.'"
                * "Example 3: 'The government
                """;

        var result = parser.parseGrammarExplanation(content);

        assertThat(result.topic()).isEqualTo(content);
        assertThat(result.summary()).isEmpty();
        assertThat(result.rules()).isEmpty();
    }

    @Test
    void returnsFallbackForTruncatedJsonObject() {
        String content = """
                {"topic": "Modal Verbs", "summary": "Modal verbs are auxiliary verbs used to express ability, permission, obligation, possibility, or advice. They do not change
                """;

        var result = parser.parseGrammarExplanation(content);

        assertThat(result.topic()).isEqualTo(content);
        assertThat(result.summary()).isEmpty();
        assertThat(result.commonMistakes()).isEmpty();
    }
}
