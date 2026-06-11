package com.englishai.ai.exception;

public class GroqAiException extends RuntimeException {

    public GroqAiException(String message) {
        super(message);
    }

    public GroqAiException(String message, Throwable cause) {
        super(message, cause);
    }
}
