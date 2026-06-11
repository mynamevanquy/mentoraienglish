package com.englishai.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConfigurationProperties(prefix = "groq.ai")
@Getter
@Setter
public class GroqAiProperties {

    private String key;
    private String url = "https://api.groq.com/openai/v1";
    private String model = "llama-3.3-70b-versatile";
    private String chatModel;
    private String exerciseModel;

    public String getChatModel() {
        return StringUtils.hasText(chatModel) ? chatModel : model;
    }

    public String getExerciseModel() {
        return StringUtils.hasText(exerciseModel) ? exerciseModel : model;
    }
}
