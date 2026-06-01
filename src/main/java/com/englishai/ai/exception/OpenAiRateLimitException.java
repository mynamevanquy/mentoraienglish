package com.englishai.ai.exception;

/**
 * Custom runtime exception representing an HTTP 429 Rate Limit error from the OpenAI API.
 */
public class OpenAiRateLimitException extends OpenAiException {

    public OpenAiRateLimitException(String message) {
        super(message);
    }

    public OpenAiRateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}
