package com.englishai.vocabulary.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SM2AlgorithmTest {

    @Test
    void forgottenWordReturnsForRelearningInAboutTenMinutes() {
        Instant before = Instant.now();

        SM2Algorithm.SM2Result result = SM2Algorithm.calculate(1, 2.50, 6, 2);

        assertThat(result.newInterval()).isZero();
        assertThat(result.newRepetitions()).isZero();
        assertThat(result.newEasinessFactor()).isEqualTo(2.30);
        assertThat(Duration.between(before, result.nextReviewAt()).toMinutes()).isBetween(9L, 10L);
    }

    @Test
    void easyNewWordGetsLongerFirstInterval() {
        SM2Algorithm.SM2Result result = SM2Algorithm.calculate(5, 2.50, 0, 0);

        assertThat(result.newInterval()).isEqualTo(4);
        assertThat(result.newRepetitions()).isEqualTo(1);
    }

    @Test
    void hardSecondRecallUsesShorterThreeDayInterval() {
        SM2Algorithm.SM2Result result = SM2Algorithm.calculate(3, 2.50, 1, 1);

        assertThat(result.newInterval()).isEqualTo(3);
        assertThat(result.newRepetitions()).isEqualTo(2);
    }

    @Test
    void rejectsUnknownRecallQuality() {
        assertThatThrownBy(() -> SM2Algorithm.calculate(6, 2.50, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
