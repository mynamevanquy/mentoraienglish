package com.englishai.exercise.enums;

import com.englishai.common.enums.ExerciseType;

public enum LearningSkill {
    LANGUAGE_USE,
    LISTENING,
    WRITING;

    public static LearningSkill from(ExerciseType exerciseType) {
        return switch (exerciseType) {
            case LISTENING -> LISTENING;
            case WRITING -> WRITING;
            case MULTIPLE_CHOICE, FILL_BLANK, SENTENCE_REORDER -> LANGUAGE_USE;
        };
    }
}
