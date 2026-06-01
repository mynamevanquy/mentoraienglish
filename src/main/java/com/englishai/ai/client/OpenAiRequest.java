package com.englishai.ai.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * OpenAI API Chat Completions request payload.
 */
public record OpenAiRequest(
    String model,
    List<Message> messages,
    double temperature,
    @JsonProperty("max_tokens") Integer maxTokens,
    @JsonProperty("response_format") ResponseFormat responseFormat
) {
    public record Message(String role, String content) {}
    public record ResponseFormat(String type) {}
}
