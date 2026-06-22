package com.englishai.common.util;

import com.englishai.common.enums.ExerciseType;
import com.englishai.common.enums.Level;
import com.englishai.exercise.enums.AssessmentStatus;
import com.englishai.exercise.enums.LearningSkill;
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

    public String assessmentStatus(AssessmentStatus status) {
        return switch (status) {
            case UNASSESSED -> "Chưa đủ dữ liệu";
            case PROVISIONAL -> "Ước lượng tạm thời";
            case ESTABLISHED -> "Đủ bằng chứng nội bộ";
        };
    }

    public String learningSkill(LearningSkill skill) {
        return switch (skill) {
            case LANGUAGE_USE -> "Ngữ pháp & từ vựng";
            case LISTENING -> "Nghe";
            case WRITING -> "Viết";
        };
    }
}
