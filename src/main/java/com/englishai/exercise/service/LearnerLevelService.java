package com.englishai.exercise.service;

import com.englishai.common.enums.AttemptStatus;
import com.englishai.common.enums.Level;
import com.englishai.exercise.config.AdaptiveLearningProperties;
import com.englishai.exercise.dto.LearnerProfileDto;
import com.englishai.exercise.entity.ExerciseAttempt;
import com.englishai.exercise.enums.AssessmentStatus;
import com.englishai.exercise.enums.LearningSkill;
import com.englishai.exercise.repository.ExerciseAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LearnerLevelService {

    private static final int SUPPORTED_SKILL_COUNT = LearningSkill.values().length;

    private final ExerciseAttemptRepository exerciseAttemptRepository;
    private final AdaptiveLearningProperties properties;

    @Transactional(readOnly = true)
    public Level determineLevel(UUID userId) {
        return getProfile(userId).level();
    }

    @Transactional(readOnly = true)
    public LearnerProfileDto getProfile(UUID userId) {
        if (!properties.isEnabled()) {
            return new LearnerProfileDto(
                    properties.getDefaultLevel(),
                    0,
                    0,
                    0,
                    0,
                    AssessmentStatus.UNASSESSED,
                    0,
                    Map.of(),
                    "Tự động đánh giá đang tắt.");
        }

        return buildProfile(loadEvidence(userId));
    }

    @Transactional(readOnly = true)
    public Map<UUID, LearnerProfileDto> getProfiles(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        if (!properties.isEnabled()) {
            Map<UUID, LearnerProfileDto> profiles = new LinkedHashMap<>();
            userIds.forEach(userId -> profiles.put(userId, disabledProfile()));
            return profiles;
        }

        Map<UUID, List<ExerciseAttempt>> attemptsByUser = new LinkedHashMap<>();
        exerciseAttemptRepository.findAssessmentEvidenceForUsers(userIds, AttemptStatus.COMPLETED)
                .forEach(attempt -> attemptsByUser
                        .computeIfAbsent(attempt.getUser().getId(), ignored -> new ArrayList<>())
                        .add(attempt));

        Map<UUID, LearnerProfileDto> profiles = new LinkedHashMap<>();
        userIds.forEach(userId -> profiles.put(
                userId,
                buildProfile(uniqueEvidence(attemptsByUser.getOrDefault(userId, Collections.emptyList())))));
        return profiles;
    }

    private LearnerProfileDto buildProfile(List<Evidence> evidence) {
        Map<LearningSkill, Double> skillScores = calculateSkillScores(evidence);
        AssessmentStatus status = determineStatus(evidence.size(), skillScores.size());

        PromotionResult advanced = evaluatePromotion(
                evidence.stream()
                        .filter(item -> item.difficulty().ordinal() >= Level.INTERMEDIATE.ordinal())
                        .toList(),
                properties.getAdvanced());
        PromotionResult intermediate = evaluatePromotion(evidence, properties.getIntermediate());

        Level level;
        PromotionResult nextPromotion;
        AdaptiveLearningProperties.PromotionRule nextRule;
        String reason;
        if (advanced.qualified()) {
            level = Level.ADVANCED;
            nextPromotion = PromotionResult.complete();
            nextRule = null;
            reason = "Đã chứng minh kết quả ổn định ở bài trung cấp/nâng cao và đủ ba nhóm kỹ năng.";
        } else if (intermediate.qualified()) {
            level = Level.INTERMEDIATE;
            nextPromotion = advanced;
            nextRule = properties.getAdvanced();
            reason = "Đã làm chủ nội dung nền tảng; cần thêm bằng chứng ở bài trung cấp để lên nâng cao.";
        } else {
            level = properties.getDefaultLevel();
            nextPromotion = intermediate;
            nextRule = properties.getIntermediate();
            reason = evidence.isEmpty()
                    ? "Chưa có dữ liệu đánh giá; hệ thống tạm dùng mức khởi đầu."
                    : "Đang thu thập thêm kết quả ở nhiều dạng bài trước khi nâng trình độ.";
        }

        return new LearnerProfileDto(
                level,
                evidence.size(),
                round(weightedAverage(evidence)),
                nextRule == null ? 0 : Math.max(0, nextRule.minimumAttempts() - nextPromotion.attemptCount()),
                nextRule == null ? 0 : nextRule.minimumAverageScore(),
                status,
                calculateConfidence(evidence.size(), skillScores.size()),
                Map.copyOf(skillScores),
                reason);
    }

    private List<Evidence> loadEvidence(UUID userId) {
        List<ExerciseAttempt> attempts = exerciseAttemptRepository.findAssessmentEvidence(
                userId,
                AttemptStatus.COMPLETED);
        return uniqueEvidence(attempts);
    }

    private List<Evidence> uniqueEvidence(List<ExerciseAttempt> attempts) {
        Map<UUID, Evidence> latestByExercise = new LinkedHashMap<>();

        for (ExerciseAttempt attempt : attempts) {
            if (!isCredibleAttempt(attempt)) {
                continue;
            }
            UUID exerciseId = attempt.getExercise().getId();
            latestByExercise.putIfAbsent(exerciseId, toEvidence(attempt));
        }
        return new ArrayList<>(latestByExercise.values());
    }

    private LearnerProfileDto disabledProfile() {
        return new LearnerProfileDto(
                properties.getDefaultLevel(),
                0,
                0,
                0,
                0,
                AssessmentStatus.UNASSESSED,
                0,
                Map.of(),
                "Tự động đánh giá đang tắt.");
    }

    private boolean isCredibleAttempt(ExerciseAttempt attempt) {
        if (attempt.getExercise() == null
                || attempt.getExercise().getDifficultyLevel() == null
                || attempt.getExercise().getExerciseType() == null
                || attempt.getScore() == null
                || attempt.getMaxScore() == null
                || attempt.getMaxScore().doubleValue() <= 0) {
            return false;
        }
        if (attempt.getTimeSpentSeconds() == null) {
            return true;
        }
        double minimumDuration = attempt.getMaxScore().doubleValue() * properties.getMinimumSecondsPerPoint();
        return attempt.getTimeSpentSeconds() >= minimumDuration;
    }

    private Evidence toEvidence(ExerciseAttempt attempt) {
        double percentage = attempt.getScore().doubleValue() * 100.0 / attempt.getMaxScore().doubleValue();
        double scoreWeight = Math.min(
                properties.getMaximumScoreWeight(),
                Math.max(1.0, attempt.getMaxScore().doubleValue()));
        return new Evidence(
                attempt.getExercise().getDifficultyLevel(),
                LearningSkill.from(attempt.getExercise().getExerciseType()),
                clamp(percentage),
                scoreWeight);
    }

    private PromotionResult evaluatePromotion(
            List<Evidence> evidence,
            AdaptiveLearningProperties.PromotionRule rule) {
        Map<LearningSkill, Double> scores = calculateSkillScores(evidence);
        boolean skillScoresPass = scores.values().stream()
                .allMatch(score -> score >= rule.minimumSkillScore());
        boolean qualified = evidence.size() >= rule.minimumAttempts()
                && weightedAverage(evidence) >= rule.minimumAverageScore()
                && scores.size() >= rule.minimumSkillAreas()
                && skillScoresPass;
        return new PromotionResult(qualified, evidence.size());
    }

    private Map<LearningSkill, Double> calculateSkillScores(List<Evidence> evidence) {
        Map<LearningSkill, List<Evidence>> grouped = new EnumMap<>(LearningSkill.class);
        evidence.forEach(item -> grouped.computeIfAbsent(item.skill(), ignored -> new ArrayList<>()).add(item));

        Map<LearningSkill, Double> scores = new EnumMap<>(LearningSkill.class);
        grouped.forEach((skill, items) -> scores.put(skill, round(weightedAverage(items))));
        return scores;
    }

    private double weightedAverage(List<Evidence> evidence) {
        if (evidence.isEmpty()) {
            return 0;
        }
        double weightedScore = 0;
        double totalWeight = 0;
        for (int index = 0; index < evidence.size(); index++) {
            Evidence item = evidence.get(index);
            double recencyWeight = index < properties.getRecentAttemptWindow()
                    ? 1.0
                    : properties.getOlderAttemptWeight();
            double weight = item.scoreWeight() * recencyWeight;
            weightedScore += item.scorePercentage() * weight;
            totalWeight += weight;
        }
        return totalWeight == 0 ? 0 : weightedScore / totalWeight;
    }

    private AssessmentStatus determineStatus(int attemptCount, int skillCount) {
        if (attemptCount < properties.getMinimumEvidenceAttempts() || skillCount < 2) {
            return AssessmentStatus.UNASSESSED;
        }
        if (attemptCount >= properties.getEstablishedMinimumAttempts()
                && skillCount == SUPPORTED_SKILL_COUNT) {
            return AssessmentStatus.ESTABLISHED;
        }
        return AssessmentStatus.PROVISIONAL;
    }

    private int calculateConfidence(int attemptCount, int skillCount) {
        int attemptConfidence = Math.min(70,
                attemptCount * 70 / Math.max(1, properties.getEstablishedMinimumAttempts()));
        int skillConfidence = skillCount * 30 / SUPPORTED_SKILL_COUNT;
        return Math.min(100, attemptConfidence + skillConfidence);
    }

    private double clamp(double score) {
        return Math.max(0, Math.min(100, score));
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private record Evidence(
            Level difficulty,
            LearningSkill skill,
            double scorePercentage,
            double scoreWeight
    ) {
    }

    private record PromotionResult(boolean qualified, int attemptCount) {
        private static PromotionResult complete() {
            return new PromotionResult(true, 0);
        }
    }
}
