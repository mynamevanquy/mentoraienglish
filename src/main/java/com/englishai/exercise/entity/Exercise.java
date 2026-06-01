package com.englishai.exercise.entity;

import com.englishai.common.entity.BaseEntity;
import com.englishai.common.enums.ExerciseSource;
import com.englishai.common.enums.ExerciseType;
import com.englishai.common.enums.Level;
import com.englishai.grammar.entity.GrammarTopic;
import com.englishai.lesson.entity.Lesson;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "exercises")
@SQLDelete(sql = "UPDATE exercises SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exercise extends BaseEntity {

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "exercise_type", nullable = false, length = 30)
    private ExerciseType exerciseType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    @Builder.Default
    private ExerciseSource source = ExerciseSource.MANUAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_lesson_id")
    private Lesson relatedLesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_grammar_id")
    private GrammarTopic relatedGrammar;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", nullable = false, length = 20)
    private Level difficultyLevel;

    @Column(name = "time_limit_seconds")
    private Integer timeLimitSeconds;

    @Column(name = "is_published", nullable = false)
    private boolean isPublished;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @OneToMany(mappedBy = "exercise", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ExerciseQuestion> questions = new ArrayList<>();

    public void addQuestion(ExerciseQuestion question) {
        questions.add(question);
        question.setExercise(this);
    }

    public void removeQuestion(ExerciseQuestion question) {
        questions.remove(question);
        question.setExercise(null);
    }
}
