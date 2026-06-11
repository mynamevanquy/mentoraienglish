package com.englishai.ai.exception;

public class GroqAiRateLimitException extends GroqAiException {

    private final String userMessage;
    private final Integer retryAfterSeconds;

    public GroqAiRateLimitException(String message) {
        this(message, null, null);
    }

    public GroqAiRateLimitException(String message, String userMessage, Integer retryAfterSeconds) {
        super(message);
        this.userMessage = userMessage;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public GroqAiRateLimitException(String message, Throwable cause) {
        super(message, cause);
        this.userMessage = null;
        this.retryAfterSeconds = null;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
