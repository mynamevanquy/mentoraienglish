package com.englishai.conversation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.englishai.common.enums.MessageRole;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponseDto {
    private UUID messageId;
    private MessageRole role;
    private String content;
    private List<GrammarCorrectionDto> corrections;
    private List<String> vocabularySuggestions;
    private Instant createdAt;
    private String conversationTitle;
}
