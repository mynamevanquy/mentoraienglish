package com.englishai.ai.exception;

/**
 * Custom runtime exception representing that the user has exceeded their hourly/daily AI request limit.
 */
public class AiRateLimitExceededException extends OpenAiException {

    public AiRateLimitExceededException(String message) {
        super(message);
    }

    public AiRateLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
