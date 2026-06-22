package com.englishai.auth.service;

import com.englishai.auth.config.PasswordResetProperties;
import com.englishai.auth.entity.PasswordResetToken;
import com.englishai.auth.repository.PasswordResetTokenRepository;
import com.englishai.user.entity.User;
import com.englishai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final PasswordResetProperties properties;
    private final JdbcTemplate jdbcTemplate;
    private final SessionRegistry sessionRegistry;

    @Transactional
    public void requestReset(String submittedEmail) {
        String email = normalizeEmail(submittedEmail);
        log.info("password_reset_request_received recipient={} mailEnabled={}",
                maskEmail(email),
                properties.isMailEnabled());
        userRepository.findByEmail(email)
                .filter(User::isEnabled)
                .ifPresent(this::createTokenAndSendEmail);
    }

    @Transactional(readOnly = true)
    public boolean isTokenValid(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }
        return tokenRepository.findByTokenHash(hash(rawToken))
                .filter(token -> token.getUser().isEnabled())
                .map(token -> token.isUsableAt(Instant.now()))
                .orElse(false);
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
        }

        Instant now = Instant.now();
        PasswordResetToken token = tokenRepository.findForUpdateByTokenHash(hash(rawToken))
                .filter(candidate -> candidate.getUser().isEnabled())
                .filter(candidate -> candidate.isUsableAt(now))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn."));

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        token.setUsedAt(now);
        userRepository.save(user);
        tokenRepository.save(token);
        tokenRepository.deleteUnusedByUser(user);

        jdbcTemplate.update("DELETE FROM persistent_logins WHERE username = ?", user.getEmail());
        expireSessions(user.getEmail());
    }

    private void createTokenAndSendEmail(User user) {
        tokenRepository.deleteUnusedByUser(user);

        String rawToken = generateToken();
        PasswordResetToken token = new PasswordResetToken(
                user,
                hash(rawToken),
                Instant.now().plus(properties.getTokenValidity()));
        tokenRepository.saveAndFlush(token);

        String resetLink = normalizedBaseUrl() + "/reset-password?token=" + rawToken;
        log.info("password_reset_token_created recipient={} expiresAt={} deliveryMode={}",
                maskEmail(user.getEmail()),
                token.getExpiresAt(),
                properties.isMailEnabled() ? "SMTP" : "LOG_ONLY");
        if (!properties.isMailEnabled()) {
            log.warn("password_reset_mail_disabled recipient={} developmentResetLink={}",
                    maskEmail(user.getEmail()),
                    resetLink);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getFromEmail());
        message.setTo(user.getEmail());
        message.setSubject("Đặt lại mật khẩu MentorAI English");
        message.setText("""
                Xin chào %s,

                Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản MentorAI English của bạn.
                Hãy mở liên kết sau để tạo mật khẩu mới:

                %s

                Liên kết có hiệu lực trong %d phút và chỉ sử dụng được một lần.
                Nếu bạn không gửi yêu cầu này, hãy bỏ qua email.
                """.formatted(
                user.getFullName(),
                resetLink,
                properties.getTokenValidity().toMinutes()));

        long startedAt = System.nanoTime();
        log.info("password_reset_mail_send_started recipient={} from={} smtpHost={} smtpPort={} smtpAuth={} startTls={}",
                maskEmail(user.getEmail()),
                properties.getFromEmail(),
                mailProperties.getHost(),
                mailProperties.getPort(),
                mailProperties.getProperties().getOrDefault("mail.smtp.auth", "false"),
                mailProperties.getProperties().getOrDefault("mail.smtp.starttls.enable", "false"));
        try {
            mailSender.send(message);
            log.info("password_reset_mail_send_succeeded recipient={} smtpHost={} durationMs={}",
                    maskEmail(user.getEmail()),
                    mailProperties.getHost(),
                    elapsedMillis(startedAt));
        } catch (MailException e) {
            Throwable rootCause = rootCause(e);
            log.error(
                    "password_reset_mail_send_failed recipient={} smtpHost={} smtpPort={} durationMs={} exception={} rootCause={} rootMessage={}",
                    maskEmail(user.getEmail()),
                    mailProperties.getHost(),
                    mailProperties.getPort(),
                    elapsedMillis(startedAt),
                    e.getClass().getSimpleName(),
                    rootCause.getClass().getSimpleName(),
                    rootCause.getMessage(),
                    e);
            throw e;
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private String normalizedBaseUrl() {
        return properties.getBaseUrl().replaceAll("/+$", "");
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String maskEmail(String email) {
        int separator = email.indexOf('@');
        if (separator <= 1) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(separator);
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private void expireSessions(String email) {
        sessionRegistry.getAllPrincipals().stream()
                .filter(principal -> email.equalsIgnoreCase(principalName(principal)))
                .flatMap(principal -> sessionRegistry.getAllSessions(principal, false).stream())
                .forEach(SessionInformation::expireNow);
    }

    private String principalName(Object principal) {
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof Principal namedPrincipal) {
            return namedPrincipal.getName();
        }
        return String.valueOf(principal);
    }
}
