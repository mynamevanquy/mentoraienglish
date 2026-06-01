package com.englishai.ai.dto;

/**
 * Result representing general text completion (e.g. from AI Tutor chat).
 */
public record CompletionResult(
    String content,
    int promptTokens,
    int completionTokens,
    int totalTokens
) {}
