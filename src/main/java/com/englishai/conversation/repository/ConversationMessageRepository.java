package com.englishai.conversation.repository;

import com.englishai.conversation.entity.ConversationMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, UUID> {
    List<ConversationMessage> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    Page<ConversationMessage> findByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

    List<ConversationMessage> findTop20ByConversationIdOrderByCreatedAtDesc(UUID conversationId);
}
