package com.englishai.conversation.entity;

import com.englishai.common.entity.BaseEntity;
import com.englishai.common.enums.ConversationStatus;
import com.englishai.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "conversations")
@SQLDelete(sql = "UPDATE conversations SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "model_used", nullable = false, length = 50)
    private String modelUsed;

    @Column(name = "total_tokens_used", nullable = false)
    @Builder.Default
    private int totalTokensUsed = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ConversationStatus status = ConversationStatus.ACTIVE;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ConversationMessage> messages = new ArrayList<>();

    public void addMessage(ConversationMessage message) {
        messages.add(message);
        message.setConversation(this);
    }

    public void removeMessage(ConversationMessage message) {
        messages.remove(message);
        message.setConversation(null);
    }
}
