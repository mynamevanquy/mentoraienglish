package com.englishai.conversation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationDetailDto {
    private UUID id;
    private String title;
    private int totalTokensUsed;
    private boolean tokenWarning;
    private List<MessageResponseDto> messages;
}
