package com.englishai.exercise.service;

import com.englishai.ai.service.AiService;
import com.englishai.common.enums.ExerciseType;
import com.englishai.exercise.entity.ExerciseQuestion;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ExerciseGraderTest {

    private final ExerciseGrader grader = new ExerciseGrader(mock(AiService.class));

    @Test
    void acceptsOneCharacterTypoForFillBlank() {
        ExerciseQuestion question = question(ExerciseType.FILL_BLANK, "beautiful");

        ExerciseGrader.GradingResult result = grader.grade(question, "beautifu", UUID.randomUUID());

        assertThat(result.correct()).isTrue();
        assertThat(result.pointsEarned()).isEqualTo(2);
    }

    @Test
    void comparesSentenceReorderArraysWithPlainSentenceAnswer() {
        ExerciseQuestion question = question(ExerciseType.SENTENCE_REORDER, "[\"She\", \"is\", \"studying\"]");

        ExerciseGrader.GradingResult result = grader.grade(question, "She is studying", UUID.randomUUID());

        assertThat(result.correct()).isTrue();
    }

    @Test
    void rejectsIncorrectSentenceOrder() {
        ExerciseQuestion question = question(ExerciseType.SENTENCE_REORDER, "She is studying");

        ExerciseGrader.GradingResult result = grader.grade(question, "Studying she is", UUID.randomUUID());

        assertThat(result.correct()).isFalse();
        assertThat(result.pointsEarned()).isZero();
    }

    private ExerciseQuestion question(ExerciseType type, String correctAnswer) {
        return ExerciseQuestion.builder()
                .questionType(type)
                .correctAnswer(correctAnswer)
                .points(2)
                .build();
    }
}
