package com.englishai.config.thymeleaf;

import com.englishai.exercise.dto.LearnerProfileDto;
import com.englishai.exercise.service.LearnerLevelService;
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
    private final LearnerLevelService learnerLevelService;

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

    @ModelAttribute("learnerProfile")
    public LearnerProfileDto learnerProfile(java.security.Principal principal) {
        if (principal == null) {
            return null;
        }
        return userRepository.findByEmail(principal.getName())
                .map(user -> learnerLevelService.getProfile(user.getId()))
                .orElse(null);
    }
}
