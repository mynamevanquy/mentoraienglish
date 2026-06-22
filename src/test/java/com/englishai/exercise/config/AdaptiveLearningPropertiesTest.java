package com.englishai.exercise.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AdaptiveLearningPropertiesTest {

    @Test
    void bindsPromotionRulesFromExternalConfiguration() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
                "app.adaptive-learning.intermediate.minimum-attempts", "7",
                "app.adaptive-learning.intermediate.minimum-average-score", "82",
                "app.adaptive-learning.intermediate.minimum-skill-areas", "3",
                "app.adaptive-learning.intermediate.minimum-skill-score", "55"));

        AdaptiveLearningProperties properties = new Binder(source)
                .bind("app.adaptive-learning", Bindable.of(AdaptiveLearningProperties.class))
                .get();

        assertThat(properties.getIntermediate().minimumAttempts()).isEqualTo(7);
        assertThat(properties.getIntermediate().minimumAverageScore()).isEqualTo(82);
        assertThat(properties.getIntermediate().minimumSkillAreas()).isEqualTo(3);
        assertThat(properties.getIntermediate().minimumSkillScore()).isEqualTo(55);
    }
}
