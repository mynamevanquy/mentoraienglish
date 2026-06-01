package com.englishai.conversation.repository;

import com.englishai.common.enums.ConversationStatus;
import com.englishai.conversation.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @Query("SELECT c FROM Conversation c WHERE c.user.id = :userId AND c.status = 'ACTIVE'")
    List<Conversation> findActiveByUser(@Param("userId") UUID userId);

    List<Conversation> findByUserIdAndStatus(UUID userId, ConversationStatus status);

    Page<Conversation> findByUserIdAndStatusAndDeletedAtIsNullOrderByUpdatedAtDesc(UUID userId, ConversationStatus status, Pageable pageable);

    long countByUserIdAndStatus(UUID userId, ConversationStatus status);
}
