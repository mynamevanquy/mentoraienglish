package com.englishai.ai.client;

import com.englishai.ai.entity.AiLog;
import com.englishai.ai.exception.OpenAiException;
import com.englishai.ai.exception.OpenAiRateLimitException;
import com.englishai.ai.repository.AiLogRepository;
import com.englishai.user.entity.User;
import com.englishai.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

/**
 * Low-level HTTP client integrating with OpenAI Chat Completions API.
 * Uses Spring {@link RestClient} and captures token usage & latency to Database.
 */
@Component
@Slf4j
public class OpenAiClient {

    private final RestClient restClient;
    private final UserRepository userRepository;
    private final AiLogRepository aiLogRepository;

    public OpenAiClient(
            @Value("${openai.api.key}") String apiKey,
            @Value("${openai.api.url:https://api.openai.com/v1}") String apiUrl,
            UserRepository userRepository,
            AiLogRepository aiLogRepository) {
        this.userRepository = userRepository;
        this.aiLogRepository = aiLogRepository;
        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Executes Chat Completions call to OpenAI API, measuring latency and logging token details.
     *
     * @param userId  The ID of the user initiating the request
     * @param request Request payload mapping model, messages, temperature, and formatting
     * @return OpenAI Chat Completions response
     * @throws OpenAiRateLimitException if rate limit is hit (HTTP 429)
     * @throws OpenAiException          for any other API or network exceptions
     */
    public OpenAiResponse postChatCompletion(UUID userId, OpenAiRequest request) {
        long startTime = System.currentTimeMillis();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        try {
            log.debug("Sending chat completion request to OpenAI for user {}. Model: {}", userId, request.model());
            OpenAiResponse response = restClient.post()
                    .uri("/chat/completions")
                    .body(request)
                    .retrieve()
                    .onStatus(status -> status.value() == 429, (req, resp) -> {
                        throw new OpenAiRateLimitException("OpenAI API rate limit exceeded (HTTP 429)");
                    })
                    .onStatus(status -> status.isError(), (req, resp) -> {
                        throw new OpenAiException("OpenAI API returned error status: " + resp.getStatusCode());
                    })
                    .body(OpenAiResponse.class);

            long latency = System.currentTimeMillis() - startTime;
            if (response != null && response.usage() != null) {
                saveLog(user, request.model(), response.usage().promptTokens(),
                        response.usage().completionTokens(), response.usage().totalTokens(),
                        latency, null);
            } else {
                saveLog(user, request.model(), 0, 0, 0, latency, "Empty response or usage details");
            }
            return response;

        } catch (OpenAiRateLimitException e) {
            long latency = System.currentTimeMillis() - startTime;
            saveLog(user, request.model(), 0, 0, 0, latency, e.getMessage());
            log.error("Rate limit hit during OpenAI call: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            saveLog(user, request.model(), 0, 0, 0, latency, e.getMessage());
            log.error("Failed to execute OpenAI request: {}", e.getMessage());
            throw new OpenAiException("Failed to call OpenAI API: " + e.getMessage(), e);
        }
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
}
