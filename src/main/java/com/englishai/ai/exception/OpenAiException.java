package com.englishai.ai.exception;

/**
 * Custom runtime exception representing an error during communication with the OpenAI API.
 */
public class OpenAiException extends RuntimeException {

    public OpenAiException(String message) {
        super(message);
    }

    public OpenAiException(String message, Throwable cause) {
        super(message, cause);
    }
}
