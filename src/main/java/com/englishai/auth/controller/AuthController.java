package com.englishai.auth.controller;

import com.englishai.auth.dto.RegisterRequest;
import com.englishai.user.entity.User;
import com.englishai.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Handles authentication-related pages: login and registration.
 * <p>
 * HTMX awareness: when the {@code HX-Request} header is present on a POST,
 * the response returns an HTML fragment (prefixed {@code fragments/}) instead of
 * performing a full-page redirect, so HTMX can swap the content inline.
 * </p>
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private static final String HX_REQUEST_HEADER = "HX-Request";

    private final UserService userService;

    /** Used to persist the security context into the session after programmatic login. */
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    // -------------------------------------------------------------------------
    // Register
    // -------------------------------------------------------------------------

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String handleRegister(
            @Valid @ModelAttribute("registerRequest") RegisterRequest request,
            BindingResult bindingResult,
            Model model,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            RedirectAttributes redirectAttributes) {

        boolean isHtmx = httpRequest.getHeader(HX_REQUEST_HEADER) != null;

        // ── Validation errors ────────────────────────────────────────────────
        if (bindingResult.hasErrors()) {
            if (isHtmx) {
                return "auth/register";
            }
            return "auth/register";
        }

        // ── Duplicate email ──────────────────────────────────────────────────
        try {
            User user = userService.registerUser(request);
            log.info("New user registered: {}", user.getEmail());

            // ── Auto-login ───────────────────────────────────────────────────
            autoLogin(user, httpRequest, httpResponse);

        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("email", "email.duplicate", e.getMessage());
            if (isHtmx) {
                return "auth/register";
            }
            return "auth/register";
        }

        if (isHtmx) {
            // Trigger client-side redirect via HTMX response header
            httpResponse.setHeader("HX-Redirect", "/dashboard");
            return "auth/register";
        }
        return "redirect:/dashboard";
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Programmatically logs in the user immediately after registration,
     * avoiding an extra round-trip to the login page.
     */
    private void autoLogin(User user, HttpServletRequest request, HttpServletResponse response) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        var authToken = UsernamePasswordAuthenticationToken
                .authenticated(user.getEmail(), null, authorities);

        var context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authToken);
        securityContextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
