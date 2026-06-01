package com.englishai.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import javax.sql.DataSource;

/**
 * Main Spring Security 6 configuration.
 * <p>
 * URL access rules:
 * <ul>
 *   <li>Public  – {@code /}, {@code /login}, {@code /register}, {@code /pricing},
 *       {@code /css/**}, {@code /js/**}, {@code /images/**}</li>
 *   <li>Authenticated – {@code /dashboard/**}, {@code /vocabulary/**}, etc.</li>
 *   <li>Admin only – {@code /admin/**}</li>
 * </ul>
 * Security features: session-based auth, CSRF, remember-me (DB), max-1-session,
 * security response headers, HTMX CSRF filter, rate-limiting filter.
 * </p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.security.remember-me-key:mentorAIDefaultKey-changeMeInProd}")
    private String rememberMeKey;

    // -------------------------------------------------------------------------
    // Security Filter Chain
    // -------------------------------------------------------------------------

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   PersistentTokenRepository tokenRepository,
                                                   SessionRegistry sessionRegistry) throws Exception {
        // CSRF – use attribute handler so Thymeleaf ${_csrf} works correctly
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();

        http
            // ── Authorization ──────────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/", "/login", "/register", "/pricing",
                        "/css/**", "/js/**", "/images/**", "/favicon.ico", "/favicon.png",
                        "/favicon-16.png", "/favicon-32.png", "/favicon-48.png",
                        "/apple-touch-icon.png",
                        "/webjars/**"
                ).permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers(
                        "/dashboard/**", "/vocabulary/**", "/grammar/**",
                        "/exercises/**", "/conversation/**", "/profile/**", "/progress/**",
                        "/settings/**"
                ).authenticated()
                .anyRequest().authenticated()
            )

            // ── Login form ──────────────────────────────────────────────────
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error")
                .permitAll()
            )

            // ── Logout ──────────────────────────────────────────────────────
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "remember-me")
                .clearAuthentication(true)
                .permitAll()
            )

            // ── Remember-me (persistent DB token store) ──────────────────────
            .rememberMe(rm -> rm
                .tokenRepository(tokenRepository)
                .tokenValiditySeconds(30 * 24 * 60 * 60) // 30 days
                .key(rememberMeKey)
                .rememberMeParameter("remember-me")
            )

            // ── Session management ──────────────────────────────────────────
            .sessionManagement(session -> session
                .maximumSessions(1)
                .expiredUrl("/login?expired")
                .sessionRegistry(sessionRegistry)
            )

            // ── CSRF ────────────────────────────────────────────────────────
            .csrf(csrf -> csrf
                .csrfTokenRequestHandler(csrfHandler)
            )

            // ── Security response headers ────────────────────────────────────
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(ct -> {})          // nosniff – enabled by default
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                )
                .referrerPolicy(ref -> ref
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
            )

            // ── Custom filters ───────────────────────────────────────────────
            // Rate-limiting filter runs before the username/password filter
            .addFilterBefore(new RateLimitingFilter(), UsernamePasswordAuthenticationFilter.class)
            // HTMX CSRF filter runs after CSRF filter to have the token attribute available
            .addFilterAfter(new HtmxCsrfFilter(),
                    org.springframework.security.web.csrf.CsrfFilter.class);

        return http.build();
    }

    // -------------------------------------------------------------------------
    // Remember-me persistent token repository (uses persistent_logins table)
    // -------------------------------------------------------------------------

    @Bean
    public PersistentTokenRepository persistentTokenRepository(DataSource dataSource) {
        JdbcTokenRepositoryImpl repo = new JdbcTokenRepositoryImpl();
        repo.setDataSource(dataSource);
        return repo;
    }

    // -------------------------------------------------------------------------
    // Session event publisher – required for concurrent-session control
    // -------------------------------------------------------------------------

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }
}
