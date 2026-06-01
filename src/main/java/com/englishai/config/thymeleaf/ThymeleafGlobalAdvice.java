package com.englishai.config.thymeleaf;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Global Thymeleaf controller advice to expose request attributes to the UI layer.
 * <p>
 * In Thymeleaf 3.1, direct utility objects like {@code #request} and
 * {@code #ctx.springRequestContext} have been removed or restricted for security reasons.
 * This class injects standard page-rendering metadata into the model globally
 * for all MVC {@link Controller}s.
 * </p>
 */
@ControllerAdvice(annotations = Controller.class)
public class ThymeleafGlobalAdvice {

    /**
     * Exposes the current request URI (e.g. {@code /dashboard}) to Thymeleaf templates
     * under the variable name {@code currentUri}.
     */
    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("username")
    public String username(java.security.Principal principal) {
        return principal != null ? principal.getName() : "Học viên";
    }
}
