package com.englishai.vocabulary.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Pure implementation of the SuperMemo 2 (SM-2) spaced repetition algorithm.
 * <p>
 * This class is intentionally kept as a stateless utility with no Spring dependencies,
 * making it easy to unit-test without a Spring context.
 * </p>
 *
 * <b>Quality scale:</b>
 * <ul>
 *   <li>0 – complete blackout, no recall at all</li>
 *   <li>1 – wrong response, correct answer remembered upon seeing</li>
 *   <li>2 – wrong response, answer seemed easy to recall</li>
 *   <li>3 – correct response recalled with serious difficulty</li>
 *   <li>4 – correct response after a hesitation</li>
 *   <li>5 – perfect response</li>
 * </ul>
 */
public final class SM2Algorithm {

    /** Minimum easiness factor — cards never become "too hard" to schedule. */
    private static final double MIN_EASINESS_FACTOR = 1.3;

    private SM2Algorithm() {
        // Utility class — no instantiation
    }

    /**
     * Result record returned by {@link #calculate}.
     *
     * @param newEasinessFactor updated EF value (≥ 1.3)
     * @param newInterval       interval in days until the next review
     * @param newRepetitions    number of consecutive correct responses
     * @param nextReviewAt      absolute timestamp for next review
     */
    public record SM2Result(
            double newEasinessFactor,
            int newInterval,
            int newRepetitions,
            Instant nextReviewAt
    ) {}

    /**
     * Applies the SM-2 algorithm and returns updated scheduling parameters.
     *
     * @param quality         user-rated quality of recall (0–5)
     * @param easinessFactor  current EF of the item (e.g. 2.5 initially)
     * @param interval        current interval in days (0 for first review)
     * @param repetitions     number of consecutive correct repetitions so far
     * @return updated SM-2 scheduling result
     */
    public static SM2Result calculate(int quality, double easinessFactor, int interval, int repetitions) {
        if (quality < 0 || quality > 5) {
            throw new IllegalArgumentException("Quality must be between 0 and 5, got: " + quality);
        }

        int newRepetitions;
        int newInterval;
        double newEF;

        Instant now = Instant.now();
        Instant nextReviewAt;

        if (quality < 3) {
            // Failed recall — show the card again soon instead of hiding it for a full day.
            newRepetitions = 0;
            newInterval = 0;
            newEF = easinessFactor - 0.20;
            nextReviewAt = now.plus(10, ChronoUnit.MINUTES);
        } else {
            newRepetitions = repetitions + 1;

            if (repetitions == 0) {
                newInterval = quality == 5 ? 4 : 1;
            } else if (repetitions == 1) {
                newInterval = switch (quality) {
                    case 3 -> 3;
                    case 5 -> 10;
                    default -> 6;
                };
            } else {
                double multiplier = switch (quality) {
                    case 3 -> 1.20;
                    case 5 -> easinessFactor * 1.30;
                    default -> easinessFactor;
                };
                newInterval = Math.max(1, (int) Math.round(interval * multiplier));
            }

            newEF = easinessFactor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
            nextReviewAt = now.plus(newInterval, ChronoUnit.DAYS);
        }

        // Enforce minimum EF
        newEF = Math.max(newEF, MIN_EASINESS_FACTOR);

        return new SM2Result(newEF, newInterval, newRepetitions, nextReviewAt);
    }
}
