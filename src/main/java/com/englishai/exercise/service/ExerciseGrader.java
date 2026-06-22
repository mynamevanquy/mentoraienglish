package com.englishai.exercise.service;

import com.englishai.ai.dto.GrammarCorrectionDto;
import com.englishai.ai.dto.GrammarCorrectionResult;
import com.englishai.ai.service.AiService;
import com.englishai.common.enums.ExerciseType;
import com.englishai.exercise.entity.ExerciseQuestion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ExerciseGrader {

    private final AiService aiService;

    public GradingResult grade(ExerciseQuestion question, String userAnswer, java.util.UUID userId) {
        ExerciseType type = question.getQuestionType();
        String expected = normalize(question.getCorrectAnswer());
        String actual = normalize(userAnswer);

        return switch (type) {
            case MULTIPLE_CHOICE -> exact(question, userAnswer, expected.equals(actual), question.getExplanation());
            case FILL_BLANK, LISTENING -> exact(question, userAnswer, isCloseEnough(expected, actual), question.getExplanation());
            case SENTENCE_REORDER -> exact(question, userAnswer, normalizeOrder(expected).equals(normalizeOrder(actual)), question.getExplanation());
            case WRITING -> gradeWriting(question, userAnswer, userId);
        };
    }

    private GradingResult exact(ExerciseQuestion question, String userAnswer, boolean correct, String feedback) {
        return new GradingResult(
                correct,
                correct ? question.getPoints() : 0,
                question.getPoints(),
                StringUtils.hasText(feedback) ? feedback : defaultFeedback(correct),
                null);
    }

    private GradingResult gradeWriting(ExerciseQuestion question, String userAnswer, java.util.UUID userId) {
        GrammarCorrectionResult result = aiService.correctGrammar(userId, userAnswer == null ? "" : userAnswer).join();
        boolean correct = !result.hasErrors();
        int earned = correct ? question.getPoints() : Math.max(0, question.getPoints() / 2);
        String feedback = result.corrections().isEmpty()
                ? result.correctedText()
                : result.corrections().stream()
                        .map(GrammarCorrectionDto::explanation)
                        .collect(Collectors.joining("\n"));
        return new GradingResult(correct, earned, question.getPoints(), question.getExplanation(), feedback);
    }

    private boolean isCloseEnough(String expected, String actual) {
        if (expected.equals(actual)) {
            return true;
        }
        return levenshtein(expected, actual) <= 1;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private String normalizeOrder(String value) {
        return normalize(value)
                .replaceAll("[\\[\\]\"']", "")
                .replaceAll("[,;|]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String defaultFeedback(boolean correct) {
        return correct ? "Correct answer." : "Review the correct answer and try the next question.";
    }

    private int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }

    public record GradingResult(
            boolean correct,
            int pointsEarned,
            int maxPoints,
            String explanation,
            String aiFeedback
    ) {
    }
}
