package com.englishai.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Propagates the CSRF token to the response header {@code X-CSRF-TOKEN} for every request
 * that carries the {@code HX-Request} header (i.e., HTMX partial requests).
 * <p>
 * HTMX can then pick it up via a meta tag or by reading the header and attaching it
 * to subsequent AJAX requests using {@code hx-headers='{"X-CSRF-TOKEN": "..."}'}.
 * </p>
 */
public class HtmxCsrfFilter extends OncePerRequestFilter {

    /** HTMX sets this header on every request it makes. */
    private static final String HX_REQUEST_HEADER = "HX-Request";

    /** Response header name that HTMX clients will read. */
    private static final String CSRF_HEADER_NAME = "X-CSRF-TOKEN";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());

        if (csrfToken != null) {
            // Force token to be loaded and session created early (lazy token in Spring Security 6)
            // This prevents "Cannot create a session after the response has been committed" in Thymeleaf.
            String token = csrfToken.getToken();
            
            if (request.getHeader(HX_REQUEST_HEADER) != null) {
                response.setHeader(CSRF_HEADER_NAME, token);
            }
        }

        filterChain.doFilter(request, response);
    }
}
