package com.englishai.ai.client;

import com.englishai.ai.config.GroqAiProperties;
import com.englishai.ai.repository.AiLogRepository;
import com.englishai.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GroqAiClientTest {

    @Test
    void rejectsMissingAndPlaceholderApiKeys() {
        assertThat(clientWithKey(null).hasUsableApiKey()).isFalse();
        assertThat(clientWithKey("").hasUsableApiKey()).isFalse();
        assertThat(clientWithKey("PUT_YOUR_GROQ_API_KEY_HERE").hasUsableApiKey()).isFalse();
    }

    @Test
    void acceptsConfiguredApiKey() {
        assertThat(clientWithKey("gsk_dummy_key_for_local_validation").hasUsableApiKey()).isTrue();
        assertThat(clientWithKey("gsk_1234567890abcdefghijklmnopqrstuvwxyz").hasUsableApiKey()).isTrue();
    }

    private GroqAiClient clientWithKey(String key) {
        GroqAiProperties properties = new GroqAiProperties();
        properties.setKey(key);
        return new GroqAiClient(
                properties,
                mock(UserRepository.class),
                mock(AiLogRepository.class),
                new ObjectMapper()
        );
    }
}
