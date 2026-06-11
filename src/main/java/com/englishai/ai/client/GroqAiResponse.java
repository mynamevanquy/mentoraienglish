package com.englishai.ai.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record GroqAiResponse(
        List<Choice> choices,
        Usage usage
) {
    public record Choice(
            Message message,
            @JsonProperty("finish_reason") String finishReason
    ) {
        public record Message(String role, String content) {}
    }

    public record Usage(
            @JsonProperty("prompt_tokens") int promptTokens,
            @JsonProperty("completion_tokens") int completionTokens,
            @JsonProperty("total_tokens") int totalTokens
    ) {}
}
