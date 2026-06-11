package com.englishai.conversation.service;

import com.englishai.ai.client.GroqAiRequest;
import com.englishai.ai.config.GroqAiProperties;
import com.englishai.ai.dto.CompletionResult;
import com.englishai.ai.exception.GroqAiException;
import com.englishai.ai.service.AiService;
import com.englishai.common.enums.ConversationStatus;
import com.englishai.common.enums.MessageRole;
import com.englishai.conversation.dto.ConversationDetailDto;
import com.englishai.conversation.dto.ConversationDto;
import com.englishai.conversation.dto.GrammarCorrectionDto;
import com.englishai.conversation.dto.MessageResponseDto;
import com.englishai.conversation.entity.Conversation;
import com.englishai.conversation.entity.ConversationMessage;
import com.englishai.conversation.repository.ConversationMessageRepository;
import com.englishai.conversation.repository.ConversationRepository;
import com.englishai.user.entity.User;
import com.englishai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private static final int MAX_CONTEXT_MESSAGES = 20;
    private static final int MAX_MONTHLY_TOKENS = 100_000;

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    private final UserRepository userRepository;
    private final AiService aiService;
    private final GroqAiProperties groqAiProperties;

    @Transactional
    public ConversationDto createConversation(UUID userId, String title) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));

        String safeTitle = StringUtils.hasText(title) ? title.trim() : "New English chat";
        Conversation conversation = Conversation.builder()
                .user(user)
                .title(safeTitle)
                .systemPrompt(buildTutorSystemPrompt("INTERMEDIATE"))
                .modelUsed(groqAiProperties.getChatModel())
                .status(ConversationStatus.ACTIVE)
                .build();

        return toConversationDto(conversationRepository.save(conversation));
    }

    @Transactional(readOnly = true)
    public Page<ConversationDto> getUserConversations(UUID userId, Pageable pageable) {
        return conversationRepository
                .findByUserIdAndStatusAndDeletedAtIsNullOrderByUpdatedAtDesc(userId, ConversationStatus.ACTIVE, pageable)
                .map(this::toConversationDto);
    }

    @Transactional(readOnly = true)
    public ConversationDetailDto getConversationWithMessages(UUID conversationId, UUID userId) {
        Conversation conversation = findOwnedConversation(conversationId, userId);
        List<MessageResponseDto> messages = conversationMessageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(this::toMessageDto)
                .toList();

        return ConversationDetailDto.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .totalTokensUsed(conversation.getTotalTokensUsed())
                .tokenWarning(isTokenWarning(conversation.getTotalTokensUsed()))
                .messages(messages)
                .build();
    }

    @Transactional(noRollbackFor = GroqAiException.class)
    public MessageResponseDto sendMessage(UUID userId, UUID conversationId, String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            throw new IllegalArgumentException("Message must not be empty.");
        }

        Conversation conversation = findOwnedConversation(conversationId, userId);
        String cleanUserMessage = userMessage.trim();

        // Automatically update title from the first message if it is the default title or blank
        String previousTitle = conversation.getTitle();
        if (previousTitle == null || previousTitle.isBlank() || "New English chat".equalsIgnoreCase(previousTitle.trim())) {
            String compactMessage = cleanUserMessage.replaceAll("\\s+", " ");
            String newTitle = compactMessage.length() > 64 ? compactMessage.substring(0, 61) + "..." : compactMessage;
            conversation.setTitle(newTitle);
        }

        List<GroqAiRequest.Message> aiMessages = buildAiMessages(conversation, cleanUserMessage);
        CompletionResult completion = aiService.completeChat(userId, aiMessages, 700);

        ConversationMessage userMessageEntity = ConversationMessage.builder()
                .conversation(conversation)
                .role(MessageRole.USER)
                .content(cleanUserMessage)
                .build();
        conversationMessageRepository.save(userMessageEntity);

        ParsedAssistantResponse parsed = parseAssistantResponse(completion.content());
        ConversationMessage assistantMessage = ConversationMessage.builder()
                .conversation(conversation)
                .role(MessageRole.ASSISTANT)
                .content(parsed.content())
                .tokensUsed(completion.totalTokens())
                .grammarCorrections(toCorrectionMetadata(parsed.corrections()))
                .vocabularySuggestions(toVocabularyMetadata(parsed.vocabularySuggestions()))
                .build();
        conversationMessageRepository.save(assistantMessage);

        conversation.setTotalTokensUsed(conversation.getTotalTokensUsed() + completion.totalTokens());
        conversationRepository.save(conversation);

        return toMessageDto(assistantMessage);
    }

    @Transactional
    public void archiveConversation(UUID conversationId, UUID userId) {
        Conversation conversation = findOwnedConversation(conversationId, userId);
        conversation.setStatus(ConversationStatus.ARCHIVED);
        conversationRepository.save(conversation);
    }

    @Transactional
    public void deleteConversation(UUID conversationId, UUID userId) {
        Conversation conversation = findOwnedConversation(conversationId, userId);
        conversation.softDelete();
        conversation.setStatus(ConversationStatus.ARCHIVED);
        conversationRepository.save(conversation);
    }

    @Transactional(readOnly = true)
    public List<MessageResponseDto> getMessages(UUID conversationId, UUID userId, Pageable pageable) {
        findOwnedConversation(conversationId, userId);
        return conversationMessageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable)
                .map(this::toMessageDto)
                .stream()
                .sorted((left, right) -> left.getCreatedAt().compareTo(right.getCreatedAt()))
                .toList();
    }

    private Conversation findOwnedConversation(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));
        if (conversation.getDeletedAt() != null || !conversation.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not have access to this conversation.");
        }
        return conversation;
    }

    private List<GroqAiRequest.Message> buildAiMessages(Conversation conversation, String userMessage) {
        List<GroqAiRequest.Message> messages = new ArrayList<>();
        messages.add(new GroqAiRequest.Message("system", conversation.getSystemPrompt()));

        List<ConversationMessage> context = new ArrayList<>(
                conversationMessageRepository.findTop20ByConversationIdOrderByCreatedAtDesc(conversation.getId()));
        Collections.reverse(context);

        int contextSize = Math.min(context.size(), MAX_CONTEXT_MESSAGES);
        for (int i = 0; i < contextSize; i++) {
            ConversationMessage message = context.get(i);
            String role = message.getRole() == MessageRole.ASSISTANT ? "assistant" : "user";
            messages.add(new GroqAiRequest.Message(role, message.getContent()));
        }

        if (context.isEmpty() || !context.getLast().getContent().equals(userMessage)) {
            messages.add(new GroqAiRequest.Message("user", userMessage));
        }

        return messages;
    }

    private String buildTutorSystemPrompt(String userLevel) {
        return """
                You are Aria, a friendly AI English tutor.
                The user's English level is %s.
                After each user message, provide a natural conversational response in English.
                When useful, add a [CORRECTIONS] section with brief grammar corrections and bilingual EN + VI explanations.
                When useful, add a [VOCABULARY] section suggesting 1-2 advanced words related to the topic.
                Keep corrections encouraging and practical. Never shame the user.
                Never break character as Aria.
                """.formatted(userLevel);
    }

    private ConversationDto toConversationDto(Conversation conversation) {
        String preview = conversationMessageRepository
                .findByConversationIdOrderByCreatedAtDesc(conversation.getId(), PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(ConversationMessage::getContent)
                .orElse("No messages yet.");

        return ConversationDto.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .totalTokensUsed((long) conversation.getTotalTokensUsed())
                .lastMessagePreview(preview)
                .build();
    }

    private MessageResponseDto toMessageDto(ConversationMessage message) {
        return MessageResponseDto.builder()
                .messageId(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .corrections(fromCorrectionMetadata(message.getGrammarCorrections()))
                .vocabularySuggestions(fromVocabularyMetadata(message.getVocabularySuggestions()))
                .createdAt(message.getCreatedAt())
                .conversationTitle(message.getConversation() != null ? message.getConversation().getTitle() : null)
                .build();
    }

    private boolean isTokenWarning(int totalTokensUsed) {
        return totalTokensUsed >= (MAX_MONTHLY_TOKENS * 0.8);
    }

    private ParsedAssistantResponse parseAssistantResponse(String rawContent) {
        String raw = rawContent == null ? "" : rawContent.trim();
        String content = sectionBeforeMarkers(raw).trim();
        List<GrammarCorrectionDto> corrections = parseCorrections(extractSection(raw, "[CORRECTIONS]", "[VOCABULARY]"));
        List<String> vocabulary = parseVocabulary(extractSection(raw, "[VOCABULARY]", null));
        return new ParsedAssistantResponse(content.isBlank() ? raw : content, corrections, vocabulary);
    }

    private String sectionBeforeMarkers(String raw) {
        int correctionsIndex = raw.indexOf("[CORRECTIONS]");
        int vocabularyIndex = raw.indexOf("[VOCABULARY]");
        int end = raw.length();
        if (correctionsIndex >= 0) {
            end = Math.min(end, correctionsIndex);
        }
        if (vocabularyIndex >= 0) {
            end = Math.min(end, vocabularyIndex);
        }
        return raw.substring(0, end);
    }

    private String extractSection(String raw, String startMarker, String nextMarker) {
        int start = raw.indexOf(startMarker);
        if (start < 0) {
            return "";
        }
        start += startMarker.length();
        int end = nextMarker != null ? raw.indexOf(nextMarker, start) : raw.length();
        if (end < 0) {
            end = raw.length();
        }
        return raw.substring(start, end).trim();
    }

    private List<GrammarCorrectionDto> parseCorrections(String section) {
        if (!StringUtils.hasText(section)) {
            return List.of();
        }

        return section.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .map(line -> line.replaceFirst("^[-*\\d.)\\s]+", ""))
                .map(line -> {
                    String original = "";
                    String corrected = "";
                    String explanation = line;
                    if (line.contains("->")) {
                        String[] parts = line.split("->", 2);
                        original = parts[0].trim();
                        explanation = parts[1].trim();
                        if (explanation.contains(":")) {
                            String[] correctedParts = explanation.split(":", 2);
                            corrected = correctedParts[0].trim();
                            explanation = correctedParts[1].trim();
                        }
                    }
                    return GrammarCorrectionDto.builder()
                            .original(original)
                            .corrected(corrected)
                            .explanation(explanation)
                            .build();
                })
                .toList();
    }

    private List<String> parseVocabulary(String section) {
        if (!StringUtils.hasText(section)) {
            return List.of();
        }

        return section.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .map(line -> line.replaceFirst("^[-*\\d.)\\s]+", ""))
                .toList();
    }

    private List<Map<String, Object>> toCorrectionMetadata(List<GrammarCorrectionDto> corrections) {
        return corrections.stream()
                .map(correction -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("original", correction.getOriginal());
                    map.put("corrected", correction.getCorrected());
                    map.put("explanation", correction.getExplanation());
                    return map;
                })
                .toList();
    }

    private List<Map<String, Object>> toVocabularyMetadata(List<String> suggestions) {
        return suggestions.stream()
                .map(suggestion -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("text", suggestion);
                    return map;
                })
                .toList();
    }

    private List<GrammarCorrectionDto> fromCorrectionMetadata(List<Map<String, Object>> metadata) {
        if (metadata == null) {
            return List.of();
        }
        return metadata.stream()
                .map(map -> GrammarCorrectionDto.builder()
                        .original((String) map.getOrDefault("original", ""))
                        .corrected((String) map.getOrDefault("corrected", ""))
                        .explanation((String) map.getOrDefault("explanation", ""))
                        .build())
                .toList();
    }

    private List<String> fromVocabularyMetadata(List<Map<String, Object>> metadata) {
        if (metadata == null) {
            return List.of();
        }
        return metadata.stream()
                .map(map -> (String) map.getOrDefault("text", ""))
                .filter(StringUtils::hasText)
                .toList();
    }

    private record ParsedAssistantResponse(
            String content,
            List<GrammarCorrectionDto> corrections,
            List<String> vocabularySuggestions
    ) {
    }
}
