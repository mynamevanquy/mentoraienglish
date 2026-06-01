package com.englishai.conversation.exception;

import com.englishai.ai.exception.AiRateLimitExceededException;
import com.englishai.ai.exception.OpenAiException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@ControllerAdvice(basePackages = "com.englishai.conversation")
public class GlobalExceptionHandler {

    private static final String HTMX_ERROR_FRAGMENT = "conversation/fragments/error-bubble :: error";
    private static final String FULL_ERROR_PAGE = "error/conversation";

    @ExceptionHandler(ResponseStatusException.class)
    public String handleResponseStatus(ResponseStatusException ex, Model model, HttpServletRequest request) {
        int status = ex.getStatusCode().value();
        String reason = ex.getReason();
        log.warn("ResponseStatusException {}: {}", status, reason);
        if (isHtmxRequest(request)) {
            model.addAttribute("message", reason);
            return HTMX_ERROR_FRAGMENT;
        }
        model.addAttribute("status", status);
        model.addAttribute("error", reason);
        return FULL_ERROR_PAGE;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException ex, Model model, HttpServletRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        if (isHtmxRequest(request)) {
            model.addAttribute("message", "Bạn không có quyền truy cập tài nguyên này.");
            return HTMX_ERROR_FRAGMENT;
        }
        model.addAttribute("status", HttpStatus.FORBIDDEN.value());
        model.addAttribute("error", "Bạn không có quyền truy cập tài nguyên này.");
        return FULL_ERROR_PAGE;
    }

    @ExceptionHandler(AiRateLimitExceededException.class)
    public String handleRateLimit(AiRateLimitExceededException ex, Model model, HttpServletRequest request) {
        log.warn("AI rate limit exceeded: {}", ex.getMessage());
        if (isHtmxRequest(request)) {
            model.addAttribute("message", ex.getMessage());
            return HTMX_ERROR_FRAGMENT;
        }
        model.addAttribute("status", HttpStatus.TOO_MANY_REQUESTS.value());
        model.addAttribute("error", ex.getMessage());
        return FULL_ERROR_PAGE;
    }

    @ExceptionHandler(OpenAiException.class)
    public String handleOpenAi(OpenAiException ex, Model model, HttpServletRequest request) {
        log.error("OpenAI error: {}", ex.getMessage(), ex);
        if (isHtmxRequest(request)) {
            model.addAttribute("message", "Dịch vụ AI tạm thời không khả dụng. Vui lòng thử lại sau.");
            return HTMX_ERROR_FRAGMENT;
        }
        model.addAttribute("status", HttpStatus.BAD_GATEWAY.value());
        model.addAttribute("error", "Dịch vụ AI tạm thời không khả dụng. Vui lòng thử lại sau.");
        return FULL_ERROR_PAGE;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException ex, Model model, HttpServletRequest request) {
        log.warn("Bad request: {}", ex.getMessage());
        if (isHtmxRequest(request)) {
            model.addAttribute("message", ex.getMessage());
            return HTMX_ERROR_FRAGMENT;
        }
        model.addAttribute("status", HttpStatus.BAD_REQUEST.value());
        model.addAttribute("error", ex.getMessage());
        return FULL_ERROR_PAGE;
    }

    @ExceptionHandler(Exception.class)
    public String handleAll(Exception ex, Model model, HttpServletRequest request) {
        log.error("Unexpected error in conversation controller", ex);
        if (isHtmxRequest(request)) {
            model.addAttribute("message", "Đã xảy ra lỗi nội bộ, vui lòng thử lại sau.");
            return HTMX_ERROR_FRAGMENT;
        }
        model.addAttribute("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        model.addAttribute("error", "Đã xảy ra lỗi nội bộ, vui lòng thử lại sau.");
        return FULL_ERROR_PAGE;
    }

    private boolean isHtmxRequest(HttpServletRequest request) {
        return "true".equals(request.getHeader("HX-Request"));
    }
}
