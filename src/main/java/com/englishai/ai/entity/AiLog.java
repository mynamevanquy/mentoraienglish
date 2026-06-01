package com.englishai.ai.entity;

import com.englishai.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity capturing logs for every AI API call.
 * Used for token usage tracking, billing, latency measurements, and rate limiting.
 */
@Entity
@Table(name = "ai_logs")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiLog {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "prompt_tokens", nullable = false, updatable = false)
    private int promptTokens;

    @Column(name = "completion_tokens", nullable = false, updatable = false)
    private int completionTokens;

    @Column(name = "total_tokens", nullable = false, updatable = false)
    private int totalTokens;

    @Column(name = "latency_ms", nullable = false, updatable = false)
    private long latencyMs;

    @Column(name = "model", nullable = false, updatable = false, length = 50)
    private String model;

    @Column(name = "error_message", updatable = false, columnDefinition = "TEXT")
    private String errorMessage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
