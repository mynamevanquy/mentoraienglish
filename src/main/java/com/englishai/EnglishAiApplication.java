package com.englishai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
public class EnglishAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnglishAiApplication.class, args);
    }

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            if (Boolean.getBoolean("flyway.clean")) {
                flyway.clean();
            }
            flyway.migrate();
        };
    }
}
