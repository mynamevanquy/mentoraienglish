package com.englishai.lesson.entity;

import com.englishai.common.entity.BaseEntity;
import com.englishai.common.enums.ContentType;
import com.englishai.course.entity.Course;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "lessons")
@SQLDelete(sql = "UPDATE lessons SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lesson extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 20)
    private ContentType contentType;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "is_published", nullable = false)
    private boolean isPublished;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LessonContent> lessonContents = new ArrayList<>();

    public void addContent(LessonContent content) {
        lessonContents.add(content);
        content.setLesson(this);
    }

    public void removeContent(LessonContent content) {
        lessonContents.remove(content);
        content.setLesson(null);
    }
}
