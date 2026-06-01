package com.englishai.conversation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationDto {
    private UUID id;
    private String title;
    private Instant createdAt;
    private Instant updatedAt;
    private Long totalTokensUsed;
    private String lastMessagePreview;
}
