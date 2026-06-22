package com.englishai.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.password-reset")
public class PasswordResetProperties {

    private String baseUrl = "http://localhost:8081";
    private Duration tokenValidity = Duration.ofMinutes(30);
    private String fromEmail = "no-reply@mentorai.local";
    private boolean mailEnabled;
}
