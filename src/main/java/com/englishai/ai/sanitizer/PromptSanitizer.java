package com.englishai.ai.sanitizer;

import com.englishai.ai.entity.AuditLog;
import com.englishai.ai.repository.AuditLogRepository;
import com.englishai.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Sanitizer component designed to detect and block or strip prompt injection patterns
 * in user inputs before constructing LLM prompts.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PromptSanitizer {

    private final AuditLogRepository auditLogRepository;

    private static final List<String> BLACKLISTED_PATTERNS = List.of(
            "ignore previous instructions",
            "forget your instructions",
            "forget the previous",
            "ignore the previous",
            "act as",
            "pretend you are",
            "you are now"
    );

    /**
     * Sanitizes user input, log security audit event in database if any injection attempt is found,
     * and strips bad patterns and characters.
     *
     * @param input     The raw user input string
     * @param user      The user initiating the request
     * @param ipAddress The IP address of the client
     * @return Cleaned and sanitized user input
     */
    public String sanitize(String input, User user, String ipAddress) {
        if (input == null || input.isBlank()) {
            return "";
        }

        String lowerInput = input.toLowerCase(Locale.ROOT);
        String cleanedInput = input;

        for (String pattern : BLACKLISTED_PATTERNS) {
            if (lowerInput.contains(pattern)) {
                log.warn("Prompt injection pattern detected: '{}' in input. Logging audit event.", pattern);
                saveAuditLog(user, ipAddress, "PROMPT_INJECTION_DETECTED",
                        String.format("Detected pattern: '%s' in user input: '%s'", pattern, input));
                
                // Strip the offending pattern to mitigate the risk
                cleanedInput = cleanedInput.replaceAll("(?i)" + pattern, "[REDACTED]");
            }
        }

        // Sanitize excessive special characters to avoid breaks in prompt formatting
        cleanedInput = cleanedInput.replaceAll("[`\\{\\}\\[\\]\\|\\^\\~\\*]", "");

        return cleanedInput.trim();
    }

    /**
     * Simple fallback sanitizer without user context.
     */
    public String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String cleaned = input;
        for (String pattern : BLACKLISTED_PATTERNS) {
            cleaned = cleaned.replaceAll("(?i)" + pattern, "[REDACTED]");
        }
        return cleaned.replaceAll("[`\\{\\}\\[\\]\\|\\^\\~\\*]", "").trim();
    }

    private void saveAuditLog(User user, String ipAddress, String eventType, String details) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .user(user)
                    .ipAddress(ipAddress != null ? ipAddress : "0.0.0.0")
                    .eventType(eventType)
                    .details(details)
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Could not write security audit log to database", e);
        }
    }
}
