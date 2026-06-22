package com.englishai.vocabulary.service;

import com.englishai.ai.dto.VocabularyInfoDto;
import com.englishai.ai.service.AiService;
import com.englishai.common.enums.Level;
import com.englishai.progress.repository.StudySessionRepository;
import com.englishai.user.entity.User;
import com.englishai.user.repository.UserRepository;
import com.englishai.vocabulary.dto.UserVocabularyDto;
import com.englishai.vocabulary.dto.VocabularyDto;
import com.englishai.vocabulary.dto.VocabularyStatsDto;
import com.englishai.vocabulary.entity.UserVocabulary;
import com.englishai.vocabulary.entity.Vocabulary;
import com.englishai.vocabulary.repository.UserVocabularyRepository;
import com.englishai.vocabulary.repository.VocabularyRepository;
import com.englishai.common.enums.ActivityType;
import com.englishai.progress.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VocabularyService {

    private static final BigDecimal DEFAULT_EASINESS_FACTOR = new BigDecimal("2.50");
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final int DAILY_NEW_WORD_LIMIT = 10;

    private final VocabularyRepository vocabularyRepository;
    private final UserVocabularyRepository userVocabularyRepository;
    private final UserRepository userRepository;
    private final AiService aiService;
    private final ProgressService progressService;
    private final StudySessionRepository studySessionRepository;

    @Transactional(readOnly = true)
    public Page<VocabularyDto> searchVocabularies(String keyword, String level, Pageable pageable) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : "";
        Page<Vocabulary> vocabularies;

        if (StringUtils.hasText(level)) {
            Level difficultyLevel = Level.valueOf(level.trim().toUpperCase());
            vocabularies = vocabularyRepository.findByWordContainingIgnoreCaseAndDifficultyLevel(
                    normalizedKeyword, difficultyLevel, pageable);
        } else {
            vocabularies = vocabularyRepository.findByWordContainingIgnoreCase(normalizedKeyword, pageable);
        }

        return vocabularies.map(VocabularyDto::from);
    }

    @Transactional(readOnly = true)
    public Page<VocabularyDto> getByLevel(Level level, Pageable pageable) {
        return vocabularyRepository.findByDifficultyLevel(level, pageable).map(VocabularyDto::from);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getLevelCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Level level : Level.values()) {
            counts.put(level.name(), vocabularyRepository.countByDifficultyLevel(level));
        }
        return counts;
    }

    @Transactional(readOnly = true)
    public VocabularyDto getById(UUID id) {
        return VocabularyDto.from(findVocabulary(id));
    }

    @Transactional(readOnly = true)
    public List<UserVocabularyDto> getRandomForReview(
            UUID userId,
            String level,
            UUID excludedVocabularyId,
            int limit) {
        Level parsedLevel = parseLevel(level);
        return userVocabularyRepository.findRandomByUserExcluding(
                        userId,
                        parsedLevel,
                        excludedVocabularyId,
                        PageRequest.of(0, Math.max(1, limit)))
                .stream()
                .map(UserVocabularyDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countDueForReview(UUID userId) {
        return userVocabularyRepository.countByUserIdAndNextReviewAtLessThanEqual(userId, Instant.now());
    }

    @Transactional(readOnly = true)
    public List<UserVocabularyDto> getDueForReview(UUID userId, int limit) {
        int safeLimit = Math.max(1, limit);
        return userVocabularyRepository.findDueForReview(userId, Instant.now(), PageRequest.of(0, safeLimit))
                .stream()
                .map(UserVocabularyDto::from)
                .toList();
    }

    @Transactional
    public UserVocabularyDto submitReview(UUID userId, UUID vocabularyId, int quality) {
        UserVocabulary userVocabulary = userVocabularyRepository.findByUserIdAndVocabularyId(userId, vocabularyId)
                .orElseThrow(() -> new IllegalArgumentException("Vocabulary is not in the user's review list."));

        SM2Algorithm.SM2Result result = SM2Algorithm.calculate(
                quality,
                userVocabulary.getSm2EasinessFactor().doubleValue(),
                userVocabulary.getSm2Interval(),
                userVocabulary.getSm2Repetitions());

        userVocabulary.setSm2EasinessFactor(BigDecimal.valueOf(result.newEasinessFactor()).setScale(2, RoundingMode.HALF_UP));
        userVocabulary.setSm2Interval(result.newInterval());
        userVocabulary.setSm2Repetitions(result.newRepetitions());
        userVocabulary.setNextReviewAt(result.nextReviewAt());
        userVocabulary.setLastReviewedAt(Instant.now());
        userVocabulary.setReviewCount(userVocabulary.getReviewCount() + 1);
        userVocabulary.setMasteryLevel(calculateMasteryLevel(quality, result.newRepetitions(), userVocabulary.getMasteryLevel()));

        UserVocabulary saved = userVocabularyRepository.save(userVocabulary);
        progressService.recordStudySession(userId, ActivityType.VOCABULARY, vocabularyId, 1, 2);

        return UserVocabularyDto.from(saved);
    }

    @Transactional
    public UserVocabularyDto addToUserList(UUID userId, UUID vocabularyId) {
        return userVocabularyRepository.findByUserIdAndVocabularyId(userId, vocabularyId)
                .map(UserVocabularyDto::from)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));
                    Vocabulary vocabulary = findVocabulary(vocabularyId);

                    UserVocabulary userVocabulary = UserVocabulary.builder()
                            .user(user)
                            .vocabulary(vocabulary)
                            .masteryLevel(0)
                            .reviewCount(0)
                            .nextReviewAt(Instant.now())
                            .sm2EasinessFactor(DEFAULT_EASINESS_FACTOR)
                            .sm2Interval(0)
                            .sm2Repetitions(0)
                            .isBookmarked(false)
                            .build();

                    return UserVocabularyDto.from(userVocabularyRepository.save(userVocabulary));
                });
    }

    @Transactional
    public UserVocabularyDto learnWord(UUID userId, UUID vocabularyId, int quality) {
        if (quality != 2 && quality != 4) {
            throw new IllegalArgumentException("Đánh giá từ mới không hợp lệ.");
        }
        var existing = userVocabularyRepository.findByUserIdAndVocabularyId(userId, vocabularyId);
        if (existing.isPresent()) {
            return UserVocabularyDto.from(existing.get());
        }
        if (getRemainingNewWordsToday(userId) == 0) {
            throw new IllegalStateException("Bạn đã hoàn thành mục tiêu từ mới hôm nay.");
        }
        addToUserList(userId, vocabularyId);
        return submitReview(userId, vocabularyId, quality);
    }

    @Transactional
    public VocabularyInfoDto getAiExplanation(UUID userId, UUID vocabularyId) {
        UserVocabulary userVocabulary = userVocabularyRepository.findByUserIdAndVocabularyId(userId, vocabularyId)
                .orElseGet(() -> {
                    addToUserList(userId, vocabularyId);
                    return userVocabularyRepository.findByUserIdAndVocabularyId(userId, vocabularyId)
                            .orElseThrow(() -> new IllegalStateException("Unable to create user vocabulary entry."));
                });

        VocabularyInfoDto cached = fromMetadata(userVocabulary.getMetadata());
        if (cached != null) {
            return cached;
        }

        VocabularyInfoDto explanation = aiService.explainVocabulary(userId, userVocabulary.getVocabulary().getWord()).join();
        userVocabulary.setMetadata(toMetadata(explanation));
        userVocabularyRepository.save(userVocabulary);
        return explanation;
    }

    @Transactional(readOnly = true)
    public Page<UserVocabularyDto> getUserVocabularies(UUID userId, Pageable pageable) {
        return userVocabularyRepository.findByUserId(userId, pageable).map(UserVocabularyDto::from);
    }

    @Transactional(readOnly = true)
    public List<VocabularyDto> getUnlearnedWords(UUID userId, String level, int limit) {
        Level parsedLevel = parseLevel(level);
        return vocabularyRepository.findUnlearnedByUser(userId, parsedLevel, PageRequest.of(0, limit))
                .stream()
                .map(VocabularyDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countUnlearnedWords(UUID userId, String level) {
        return vocabularyRepository.countUnlearnedByUser(userId, parseLevel(level));
    }

    @Transactional(readOnly = true)
    public long countWordsLearnedToday(UUID userId) {
        LocalDate today = LocalDate.now(DEFAULT_ZONE);
        Instant from = today.atStartOfDay(DEFAULT_ZONE).toInstant();
        Instant to = today.plusDays(1).atStartOfDay(DEFAULT_ZONE).toInstant();
        return userVocabularyRepository.countByUserIdAndCreatedAtBetween(userId, from, to);
    }

    @Transactional(readOnly = true)
    public int getRemainingNewWordsToday(UUID userId) {
        return Math.max(0, DAILY_NEW_WORD_LIMIT - (int) countWordsLearnedToday(userId));
    }

    @Transactional(readOnly = true)
    public VocabularyStatsDto getDetailedStats(UUID userId) {
        long totalWords = userVocabularyRepository.countByUserId(userId);
        long masteredWords = userVocabularyRepository.countByUserIdAndMasteryLevelGreaterThanEqual(userId, 4);
        long dueReviewCount = userVocabularyRepository.countByUserIdAndNextReviewAtLessThanEqual(userId, Instant.now());
        long totalReviews = userVocabularyRepository.sumReviewCountByUserId(userId);

        // Mastery distribution
        Map<String, Long> masteryDistribution = getMasteryStats(userId);

        // Words added over time (last 30 days)
        LocalDate today = LocalDate.now(DEFAULT_ZONE);
        LocalDate from = today.minusDays(30);
        Map<LocalDate, Long> wordsAddedOverTime = new LinkedHashMap<>();
        LocalDate current = from;
        while (!current.isAfter(today)) {
            wordsAddedOverTime.put(current, 0L);
            current = current.plusDays(1);
        }
        for (Object[] row : userVocabularyRepository.countWordsAddedByDate(userId, DEFAULT_ZONE.getId())) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            long count = ((Number) row[1]).longValue();
            wordsAddedOverTime.put(date, count);
        }

        // Reviews over time (last 30 days)
        Instant startInstant = from.atStartOfDay(DEFAULT_ZONE).toInstant();
        Instant endInstant = today.plusDays(1).atStartOfDay(DEFAULT_ZONE).toInstant();
        Map<LocalDate, Long> reviewsOverTime = new LinkedHashMap<>();
        current = from;
        while (!current.isAfter(today)) {
            reviewsOverTime.put(current, 0L);
            current = current.plusDays(1);
        }
        for (Object[] row : studySessionRepository.findDailyVocabularyReviews(userId, startInstant, endInstant, DEFAULT_ZONE.getId())) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            long count = ((Number) row[1]).longValue();
            reviewsOverTime.put(date, count);
        }

        return VocabularyStatsDto.builder()
                .totalWords(totalWords)
                .masteredWords(masteredWords)
                .dueReviewCount(dueReviewCount)
                .totalReviews(totalReviews)
                .masteryDistribution(masteryDistribution)
                .wordsAddedOverTime(wordsAddedOverTime)
                .reviewsOverTime(reviewsOverTime)
                .build();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getMasteryStats(UUID userId) {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (int level = 0; level <= 5; level++) {
            stats.put(masteryLabel(level), 0L);
        }

        for (Object[] row : userVocabularyRepository.countByMasteryLevel(userId)) {
            int masteryLevel = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            stats.put(masteryLabel(masteryLevel), count);
        }

        return stats;
    }

    private Vocabulary findVocabulary(UUID id) {
        return vocabularyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Vocabulary not found: " + id));
    }

    private Level parseLevel(String level) {
        if (!StringUtils.hasText(level)) {
            return null;
        }
        try {
            return Level.valueOf(level.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private int calculateMasteryLevel(int quality, int repetitions, int currentMasteryLevel) {
        if (quality < 3) {
            return Math.max(0, currentMasteryLevel - 1);
        }
        return Math.min(5, Math.max(currentMasteryLevel, repetitions));
    }

    private String masteryLabel(int level) {
        return switch (level) {
            case 0 -> "Mới";
            case 1 -> "Đang học";
            case 2 -> "Quen thuộc";
            case 3 -> "Tốt";
            case 4 -> "Thành thạo";
            default -> "Chuyên gia";
        };
    }

    private Map<String, Object> toMetadata(VocabularyInfoDto dto) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("word", dto.word());
        metadata.put("ipa", dto.ipa());
        metadata.put("partOfSpeech", dto.partOfSpeech());
        metadata.put("vietnameseMeaning", dto.vietnameseMeaning());
        metadata.put("definition", dto.definition());
        metadata.put("examples", dto.examples());
        metadata.put("synonyms", dto.synonyms());
        metadata.put("antonyms", dto.antonyms());
        return metadata;
    }

    @SuppressWarnings("unchecked")
    private VocabularyInfoDto fromMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }

        return new VocabularyInfoDto(
                (String) metadata.getOrDefault("word", ""),
                (String) metadata.getOrDefault("ipa", ""),
                (String) metadata.getOrDefault("partOfSpeech", ""),
                (String) metadata.getOrDefault("vietnameseMeaning", ""),
                (String) metadata.getOrDefault("definition", ""),
                (List<String>) metadata.getOrDefault("examples", List.of()),
                (List<String>) metadata.getOrDefault("synonyms", List.of()),
                (List<String>) metadata.getOrDefault("antonyms", List.of()));
    }
}
