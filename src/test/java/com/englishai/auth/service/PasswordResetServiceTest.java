package com.englishai.auth.service;

import com.englishai.auth.config.PasswordResetProperties;
import com.englishai.auth.entity.PasswordResetToken;
import com.englishai.auth.repository.PasswordResetTokenRepository;
import com.englishai.user.entity.Role;
import com.englishai.user.entity.User;
import com.englishai.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JavaMailSender mailSender;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private SessionRegistry sessionRegistry;

    private PasswordResetService service;
    private PasswordResetProperties properties;
    private MailProperties mailProperties;

    @BeforeEach
    void setUp() {
        properties = new PasswordResetProperties();
        properties.setBaseUrl("https://mentor.example/");
        properties.setTokenValidity(Duration.ofMinutes(30));
        properties.setFromEmail("no-reply@mentor.example");
        properties.setMailEnabled(true);
        mailProperties = new MailProperties();
        mailProperties.setHost("smtp.example.com");
        mailProperties.setPort(587);
        mailProperties.getProperties().put("mail.smtp.auth", "true");
        mailProperties.getProperties().put("mail.smtp.starttls.enable", "true");
        service = new PasswordResetService(
                userRepository,
                tokenRepository,
                passwordEncoder,
                mailSender,
                mailProperties,
                properties,
                jdbcTemplate,
                sessionRegistry);
    }

    @Test
    void createsHashedOneTimeTokenAndSendsRawTokenOnlyByEmail() throws Exception {
        User user = enabledUser();
        when(userRepository.findByEmail("learner@example.com")).thenReturn(Optional.of(user));

        service.requestReset(" Learner@Example.com ");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).deleteUnusedByUser(user);
        verify(tokenRepository).saveAndFlush(tokenCaptor.capture());

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        String body = messageCaptor.getValue().getText();
        String rawToken = body.substring(body.indexOf("?token=") + 7).lines().findFirst().orElseThrow();

        assertThat(tokenCaptor.getValue().getTokenHash()).isEqualTo(sha256(rawToken));
        assertThat(tokenCaptor.getValue().getTokenHash()).doesNotContain(rawToken);
        assertThat(messageCaptor.getValue().getTo()).containsExactly("learner@example.com");
        assertThat(body).contains("https://mentor.example/reset-password?token=");
    }

    @Test
    void unknownEmailDoesNotRevealItselfThroughMailActivity() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        service.requestReset("missing@example.com");

        verify(tokenRepository, never()).saveAndFlush(any());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void localModeCreatesTokenWithoutConnectingToSmtp() {
        User user = enabledUser();
        when(userRepository.findByEmail("learner@example.com")).thenReturn(Optional.of(user));
        properties.setMailEnabled(false);

        service.requestReset("learner@example.com");

        verify(tokenRepository).saveAndFlush(any(PasswordResetToken.class));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void propagatesSmtpFailureAfterLoggingDeliveryContext() {
        User user = enabledUser();
        when(userRepository.findByEmail("learner@example.com")).thenReturn(Optional.of(user));
        MailSendException failure = new MailSendException(
                "SMTP delivery failed",
                new IllegalStateException("Connection refused"));
        org.mockito.Mockito.doThrow(failure)
                .when(mailSender)
                .send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> service.requestReset("learner@example.com"))
                .isSameAs(failure);
    }

    @Test
    void resetsPasswordConsumesTokenAndRevokesRememberMe() {
        User user = enabledUser();
        PasswordResetToken token = new PasswordResetToken(user, sha256("raw-token"), Instant.now().plusSeconds(300));
        when(tokenRepository.findForUpdateByTokenHash(sha256("raw-token"))).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-password");
        when(sessionRegistry.getAllPrincipals()).thenReturn(List.of());

        service.resetPassword("raw-token", "new-password");

        assertThat(user.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(token.getUsedAt()).isNotNull();
        verify(tokenRepository).deleteUnusedByUser(user);
        verify(jdbcTemplate).update(
                "DELETE FROM persistent_logins WHERE username = ?",
                "learner@example.com");
    }

    @Test
    void rejectsExpiredTokenWithoutChangingPassword() {
        User user = enabledUser();
        PasswordResetToken token = new PasswordResetToken(user, sha256("expired"), Instant.now().minusSeconds(1));
        when(tokenRepository.findForUpdateByTokenHash(sha256("expired"))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.resetPassword("expired", "new-password"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hết hạn");

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    private User enabledUser() {
        return User.builder()
                .fullName("Learner")
                .email("learner@example.com")
                .passwordHash("old-password")
                .role(Role.USER)
                .enabled(true)
                .build();
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
