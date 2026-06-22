package com.englishai.exercise.config;

import com.englishai.common.enums.Level;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.adaptive-learning")
public class AdaptiveLearningProperties {

    private boolean enabled = true;
    private Level defaultLevel = Level.BEGINNER;
    private int minimumEvidenceAttempts = 3;
    private int establishedMinimumAttempts = 10;
    private int recentAttemptWindow = 10;
    private double olderAttemptWeight = 0.5;
    private int maximumScoreWeight = 10;
    private double minimumSecondsPerPoint = 2.0;
    private PromotionRule intermediate = new PromotionRule(5, 80.0, 2, 50.0);
    private PromotionRule advanced = new PromotionRule(8, 80.0, 3, 60.0);

    public record PromotionRule(
            int minimumAttempts,
            double minimumAverageScore,
            int minimumSkillAreas,
            double minimumSkillScore
    ) {
    }
}
