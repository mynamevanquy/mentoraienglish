package com.englishai.ai.service;

import com.englishai.ai.config.GroqAiProperties;
import com.englishai.ai.client.GroqAiClient;
import com.englishai.ai.client.GroqAiRequest;
import com.englishai.ai.client.GroqAiResponse;
import com.englishai.ai.dto.*;
import com.englishai.ai.exception.AiRateLimitExceededException;
import com.englishai.ai.exception.GroqAiException;
import com.englishai.common.enums.MessageRole;
import com.englishai.conversation.entity.Conversation;
import com.englishai.conversation.entity.ConversationMessage;
import com.englishai.conversation.repository.ConversationMessageRepository;
import com.englishai.conversation.repository.ConversationRepository;
import com.englishai.exercise.service.LearnerLevelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final GroqAiClient groqAiClient;
    private final PromptBuilder promptBuilder;
    private final AiResponseParser aiResponseParser;
    private final TokenUsageTracker tokenUsageTracker;
    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    private final GroqAiProperties groqAiProperties;
    private final LearnerLevelService learnerLevelService;

    private void checkRateLimit(UUID userId) {
        if (!tokenUsageTracker.canMakeRequest(userId)) {
            log.warn("User {} exceeded their hourly AI request limit.", userId);
            throw new AiRateLimitExceededException("Bạn đã vượt quá giới hạn 20 yêu cầu AI mỗi giờ.");
        }
    }

    public CompletionResult completeChat(UUID userId, List<GroqAiRequest.Message> messages, int maxTokens) {
        checkRateLimit(userId);

        GroqAiRequest request = new GroqAiRequest(
                groqAiProperties.getChatModel(),
                messages,
                0.7,
                maxTokens
        );

        GroqAiResponse response = groqAiClient.generateContent(userId, request);
        if (response == null || response.choices().isEmpty()) {
            throw new GroqAiException("Empty response from Groq AI");
        }

        String reply = response.choices().getFirst().message().content();
        int promptTokens = response.usage() != null ? response.usage().promptTokens() : 0;
        int completionTokens = response.usage() != null ? response.usage().completionTokens() : 0;
        int totalTokens = response.usage() != null ? response.usage().totalTokens() : 0;

        return new CompletionResult(reply, promptTokens, completionTokens, totalTokens);
    }

    @Async
    @Transactional
    public CompletableFuture<CompletionResult> chat(UUID userId, UUID conversationId, String userMessage) {
        try {
            checkRateLimit(userId);

            Conversation conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));

            // Load and build history
            List<ConversationMessage> historyMessages = conversationMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
            StringBuilder historyStr = new StringBuilder();
            for (ConversationMessage msg : historyMessages) {
                historyStr.append(msg.getRole().name()).append(": ").append(msg.getContent()).append("\n");
            }

            // Save user message to DB
            ConversationMessage userMsgEntity = ConversationMessage.builder()
                    .conversation(conversation)
                    .role(MessageRole.USER)
                    .content(userMessage)
                    .build();
            conversationMessageRepository.save(userMsgEntity);

            // Construct prompt
            String systemPrompt = promptBuilder.buildTutorPrompt(
                    historyStr.toString(),
                    userMessage,
                    learnerLevelService.determineLevel(userId).name());

            List<GroqAiRequest.Message> messages = new ArrayList<>();
            messages.add(new GroqAiRequest.Message("system", systemPrompt));
            messages.add(new GroqAiRequest.Message("user", userMessage));

            GroqAiRequest request = new GroqAiRequest(
                    groqAiProperties.getChatModel(),
                    messages,
                    0.7,
                    1000
            );

            GroqAiResponse response = groqAiClient.generateContent(userId, request);
            if (response == null || response.choices().isEmpty()) {
                throw new GroqAiException("Empty response from Groq AI");
            }

            String reply = response.choices().getFirst().message().content();
            int promptTokens = response.usage() != null ? response.usage().promptTokens() : 0;
            int completionTokens = response.usage() != null ? response.usage().completionTokens() : 0;
            int totalTokens = response.usage() != null ? response.usage().totalTokens() : 0;

            // Save assistant message to DB
            ConversationMessage assistantMsgEntity = ConversationMessage.builder()
                    .conversation(conversation)
                    .role(MessageRole.ASSISTANT)
                    .content(reply)
                    .tokensUsed(totalTokens)
                    .build();
            conversationMessageRepository.save(assistantMsgEntity);

            // Update conversation token count
            conversation.setTotalTokensUsed(conversation.getTotalTokensUsed() + totalTokens);
            conversationRepository.save(conversation);

            return CompletableFuture.completedFuture(
                    new CompletionResult(reply, promptTokens, completionTokens, totalTokens)
            );

        } catch (Exception e) {
            log.error("Error in AI chat execution", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async
    public CompletableFuture<List<ExerciseQuestionDto>> generateExercises(UUID userId, GenerateExerciseRequest request) {
        try {
            checkRateLimit(userId);

            String prompt = promptBuilder.buildExerciseGeneratorPrompt(
                    request.topic(),
                    request.exerciseType(),
                    request.difficulty(),
                    request.count()
            );

            GroqAiRequest apiRequest = new GroqAiRequest(
                    groqAiProperties.getExerciseModel(),
                    List.of(new GroqAiRequest.Message("user", prompt)),
                    0.5,
                    2000
            );

            GroqAiResponse response = groqAiClient.generateContent(userId, apiRequest);
            if (response == null || response.choices().isEmpty()) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            }

            String content = response.choices().getFirst().message().content();
            List<ExerciseQuestionDto> questions = aiResponseParser.parseExerciseList(content);
            return CompletableFuture.completedFuture(questions);

        } catch (Exception e) {
            log.error("Failed to generate exercises via AI", e);
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
    }

    @Async
    public CompletableFuture<GrammarExplanationDto> explainGrammar(UUID userId, String grammarTopic, String userLevel) {
        try {
            checkRateLimit(userId);

            String prompt = promptBuilder.buildGrammarExplanationPrompt(grammarTopic, userLevel);

            GroqAiRequest apiRequest = new GroqAiRequest(
                    groqAiProperties.getModel(),
                    List.of(new GroqAiRequest.Message("user", prompt)),
                    0.3,
                    4000
            );

            GroqAiResponse response = groqAiClient.generateContent(userId, apiRequest);
            if (response == null || response.choices().isEmpty()) {
                return CompletableFuture.completedFuture(
                        new GrammarExplanationDto(grammarTopic, "Không nhận được phản hồi từ AI.", Collections.emptyList(), Collections.emptyList(), Collections.emptyList())
                );
            }

            String content = response.choices().getFirst().message().content();
            GrammarExplanationDto explanation = aiResponseParser.parseGrammarExplanation(content);
            return CompletableFuture.completedFuture(explanation);

        } catch (Exception e) {
            log.error("Failed to explain grammar via AI", e);
            return CompletableFuture.completedFuture(
                    new GrammarExplanationDto(grammarTopic, "Có lỗi xảy ra khi gọi AI: " + e.getMessage(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList())
            );
        }
    }

    @Async
    public CompletableFuture<VocabularyInfoDto> explainVocabulary(UUID userId, String word) {
        try {
            checkRateLimit(userId);

            String prompt = promptBuilder.buildVocabularyExplanationPrompt(
                    word,
                    learnerLevelService.determineLevel(userId).name());

            GroqAiRequest apiRequest = new GroqAiRequest(
                    groqAiProperties.getModel(),
                    List.of(new GroqAiRequest.Message("user", prompt)),
                    0.3,
                    1200
            );

            GroqAiResponse response = groqAiClient.generateContent(userId, apiRequest);
            if (response == null || response.choices().isEmpty()) {
                return CompletableFuture.completedFuture(
                        new VocabularyInfoDto(word, "", "", "", "Không nhận được phản hồi từ AI.", Collections.emptyList(), Collections.emptyList(), Collections.emptyList())
                );
            }

            String content = response.choices().getFirst().message().content();
            VocabularyInfoDto vocabInfo = aiResponseParser.parseVocabularyInfo(content);
            return CompletableFuture.completedFuture(vocabInfo);

        } catch (Exception e) {
            log.error("Failed to explain vocabulary via AI", e);
            return CompletableFuture.completedFuture(
                    new VocabularyInfoDto(word, "", "", "", "Có lỗi xảy ra khi gọi AI: " + e.getMessage(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList())
            );
        }
    }

    @Async
    public CompletableFuture<GrammarCorrectionResult> correctGrammar(UUID userId, String text) {
        try {
            checkRateLimit(userId);

            String prompt = promptBuilder.buildGrammarCorrectionPrompt(
                    text,
                    learnerLevelService.determineLevel(userId).name());

            GroqAiRequest apiRequest = new GroqAiRequest(
                    groqAiProperties.getModel(),
                    List.of(new GroqAiRequest.Message("user", prompt)),
                    0.2,
                    1500
            );

            GroqAiResponse response = groqAiClient.generateContent(userId, apiRequest);
            if (response == null || response.choices().isEmpty()) {
                return CompletableFuture.completedFuture(
                        new GrammarCorrectionResult(text, text, Collections.emptyList(), false)
                );
            }

            String content = response.choices().getFirst().message().content();
            GrammarCorrectionResult correction = aiResponseParser.parseGrammarCorrections(content);
            return CompletableFuture.completedFuture(correction);

        } catch (Exception e) {
            log.error("Failed to check grammar via AI", e);
            return CompletableFuture.completedFuture(
                    new GrammarCorrectionResult(text, text, Collections.emptyList(), false)
            );
        }
    }
}
