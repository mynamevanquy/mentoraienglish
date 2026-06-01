package com.englishai.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Provides the {@link PasswordEncoder} bean in a standalone configuration class.
 * Keeping it separate from {@code SecurityConfig} and {@code UserDetailsServiceImpl}
 * prevents the circular-dependency that Spring would otherwise detect when
 * those two beans reference each other through {@code AuthenticationManagerBuilder}.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
