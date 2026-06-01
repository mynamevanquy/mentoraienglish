package com.englishai.common.audit;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Provides the current user identifier for audit fields.
 * In a real application this would extract the username from the security context.
 * For now it defaults to "system" when no authentication is present.
 */
@Component
public class AuditorAwareImpl implements AuditorAware<String> {
    @Override
    public Optional<String> getCurrentAuditor() {
        // TODO: Integrate with Spring Security to return the authenticated principal name.
        return Optional.of("system");
    }
}
