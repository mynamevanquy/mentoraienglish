package com.englishai.ai.client;

import com.englishai.ai.config.GroqAiProperties;
import com.englishai.ai.entity.AiLog;
import com.englishai.ai.exception.GroqAiException;
import com.englishai.ai.exception.GroqAiRateLimitException;
import com.englishai.ai.repository.AiLogRepository;
import com.englishai.user.entity.User;
import com.englishai.user.repository.UserRepository;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class GroqAiClient {

    private static final String PLACEHOLDER_KEY = "PUT_YOUR_GROQ_API_KEY_HERE";

    private final RestClient restClient;
    private final GroqAiProperties groqAiProperties;
    private final UserRepository userRepository;
    private final AiLogRepository aiLogRepository;
    private final ObjectMapper objectMapper;

    public GroqAiClient(
            GroqAiProperties groqAiProperties,
            UserRepository userRepository,
            AiLogRepository aiLogRepository,
            ObjectMapper objectMapper) {
        this.groqAiProperties = groqAiProperties;
        this.userRepository = userRepository;
        this.aiLogRepository = aiLogRepository;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(groqAiProperties.getUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        log.info("Groq AI configured. url={}, model={}, key={}",
                groqAiProperties.getUrl(),
                groqAiProperties.getModel(),
                redactKey(groqAiProperties.getKey()));
    }

    public GroqAiResponse generateContent(UUID userId, GroqAiRequest request) {
        long startTime = System.currentTimeMillis();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        try {
            if (!hasUsableApiKey()) {
                throw new GroqAiException("Missing Groq API key. Configure groq.ai.key or GROQ_API_KEY.");
            }

            log.debug("Sending Groq chat completion request for user {}. Model: {}", userId, request.model());
            GroqAiResponse response = postChatCompletion(request);
            long latency = System.currentTimeMillis() - startTime;

            if (response != null && response.usage() != null) {
                saveLog(user, request.model(), response.usage().promptTokens(),
                        response.usage().completionTokens(), response.usage().totalTokens(),
                        latency, null);
            } else {
                saveLog(user, request.model(), 0, 0, 0, latency, "Empty response or usage details");
            }
            return response;

        } catch (GroqAiRateLimitException e) {
            long latency = System.currentTimeMillis() - startTime;
            saveLog(user, request.model(), 0, 0, 0, latency, e.getMessage());
            log.warn("Rate limit hit during Groq AI call: {}", e.getMessage());
            throw e;
        } catch (GroqAiException e) {
            long latency = System.currentTimeMillis() - startTime;
            saveLog(user, request.model(), 0, 0, 0, latency, e.getMessage());
            log.error("Groq AI request failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            saveLog(user, request.model(), 0, 0, 0, latency, e.getMessage());
            log.error("Failed to execute Groq AI request: {}", e.getMessage());
            throw new GroqAiException("Failed to call Groq AI: " + e.getMessage(), e);
        }
    }

    private GroqAiResponse postChatCompletion(GroqAiRequest request) {
        return restClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + groqAiProperties.getKey())
                .body(toGroqRequest(request))
                .retrieve()
                .onStatus(status -> status.value() == 429, (req, resp) -> {
                    String errorBody = StreamUtils.copyToString(resp.getBody(), StandardCharsets.UTF_8);
                    Integer retryAfterSeconds = parseRetryAfterSeconds(resp.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
                    throw buildRateLimitException(errorBody, retryAfterSeconds);
                })
                .onStatus(status -> status.isError(), (req, resp) -> {
                    String errorBody = StreamUtils.copyToString(resp.getBody(), StandardCharsets.UTF_8);
                    throw new GroqAiException("Groq AI returned error status: " + resp.getStatusCode() + ". Body: " + errorBody);
                })
                .body(GroqAiResponse.class);
    }

    private GroqChatCompletionRequest toGroqRequest(GroqAiRequest request) {
        return new GroqChatCompletionRequest(
                request.model(),
                request.messages(),
                request.temperature(),
                request.maxTokens()
        );
    }

    private void saveLog(User user, String model, int promptTokens, int completionTokens,
                         int totalTokens, long latencyMs, String errorMessage) {
        try {
            AiLog aiLog = AiLog.builder()
                    .user(user)
                    .model(model)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .totalTokens(totalTokens)
                    .latencyMs(latencyMs)
                    .errorMessage(errorMessage)
                    .build();
            aiLogRepository.save(aiLog);
            log.debug("Saved AI log entry for user {}. Total tokens: {}, Latency: {}ms",
                    user.getId(), totalTokens, latencyMs);
        } catch (Exception ex) {
            log.error("Could not write AI log to database", ex);
        }
    }

    private GroqAiRateLimitException buildRateLimitException(String errorBody, Integer retryAfterSeconds) {
        String providerMessage = parseProviderMessage(errorBody);
        StringBuilder userMessage = new StringBuilder(
                "Groq dang gioi han quota hoac toc do goi API cua key hien tai.");
        if (retryAfterSeconds != null) {
            userMessage.append(" Hay thu lai sau khoang ").append(retryAfterSeconds).append(" giay.");
        }
        userMessage.append(" Kiem tra quota va billing trong Groq Console.");

        return new GroqAiRateLimitException(
                "Groq AI rate limit exceeded (HTTP 429): " + errorBody,
                StringUtils.hasText(providerMessage) ? providerMessage : userMessage.toString(),
                retryAfterSeconds
        );
    }

    private String parseProviderMessage(String errorBody) {
        try {
            JsonNode root = objectMapper.readTree(errorBody);
            return root.path("error").path("message").asText("");
        } catch (Exception ex) {
            log.warn("Could not parse Groq rate limit body: {}", ex.getMessage());
            return "";
        }
    }

    private Integer parseRetryAfterSeconds(String retryAfter) {
        if (!StringUtils.hasText(retryAfter)) {
            return null;
        }
        try {
            return Math.max(1, (int) Math.ceil(Double.parseDouble(retryAfter.trim())));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    boolean hasUsableApiKey() {
        String key = groqAiProperties.getKey();
        return StringUtils.hasText(key)
                && !PLACEHOLDER_KEY.equals(key);
    }

    private String redactKey(String key) {
        if (!StringUtils.hasText(key)) {
            return "<empty>";
        }
        if (key.length() <= 10) {
            return "<too-short>";
        }
        return key.substring(0, 6) + "..." + key.substring(key.length() - 4);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record GroqChatCompletionRequest(
            String model,
            List<GroqAiRequest.Message> messages,
            double temperature,
            @JsonProperty("max_completion_tokens") Integer maxCompletionTokens
    ) {}
}
