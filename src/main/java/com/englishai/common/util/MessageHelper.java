package com.englishai.common.util;

import com.englishai.common.enums.ExerciseType;
import com.englishai.common.enums.Level;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Simple utility to convert enum constants to Vietnamese display strings.
 */
@Component
public class MessageHelper {
    private static final Map<ExerciseType, String> EXERCISE_TYPE_MAP = Map.of(
            ExerciseType.MULTIPLE_CHOICE, "Trắc nghiệm",
            ExerciseType.FILL_BLANK, "Điền vào chỗ trống",
            ExerciseType.SENTENCE_REORDER, "Sắp xếp câu",
            ExerciseType.WRITING, "Viết",
            ExerciseType.LISTENING, "Nghe"
    );

    private static final Map<Level, String> LEVEL_MAP = Map.of(
            Level.BEGINNER, "Mới bắt đầu",
            Level.INTERMEDIATE, "Trung cấp",
            Level.ADVANCED, "Nâng cao"
    );

    public String exerciseType(ExerciseType type) {
        return EXERCISE_TYPE_MAP.getOrDefault(type, type.name());
    }

    public String level(Level level) {
        return LEVEL_MAP.getOrDefault(level, level.name());
    }
}
