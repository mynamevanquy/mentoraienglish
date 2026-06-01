package com.englishai.config.thymeleaf;

import com.englishai.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Global Thymeleaf controller advice to expose request attributes to the UI layer.
 */
@ControllerAdvice(annotations = Controller.class)
@RequiredArgsConstructor
public class ThymeleafGlobalAdvice {

    private final UserRepository userRepository;

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("username")
    public String username(java.security.Principal principal) {
        if (principal == null) {
            return "Học viên";
        }

        return userRepository.findByEmail(principal.getName())
                .map(user -> user.getFullName())
                .orElse(principal.getName());
    }

    @ModelAttribute("isAdmin")
    public boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
