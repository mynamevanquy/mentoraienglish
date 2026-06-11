package com.englishai.ai.client;

import java.util.List;

public record GroqAiRequest(
        String model,
        List<Message> messages,
        double temperature,
        Integer maxTokens
) {
    public record Message(String role, String content) {}
}
