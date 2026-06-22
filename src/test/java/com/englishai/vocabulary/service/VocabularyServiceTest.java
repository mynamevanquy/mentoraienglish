package com.englishai.vocabulary.service;

import com.englishai.ai.service.AiService;
import com.englishai.progress.repository.StudySessionRepository;
import com.englishai.progress.service.ProgressService;
import com.englishai.user.repository.UserRepository;
import com.englishai.vocabulary.repository.UserVocabularyRepository;
import com.englishai.vocabulary.repository.VocabularyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VocabularyServiceTest {

    @Mock private VocabularyRepository vocabularyRepository;
    @Mock private UserVocabularyRepository userVocabularyRepository;
    @Mock private UserRepository userRepository;
    @Mock private AiService aiService;
    @Mock private ProgressService progressService;
    @Mock private StudySessionRepository studySessionRepository;

    private VocabularyService vocabularyService;

    @BeforeEach
    void setUp() {
        vocabularyService = new VocabularyService(
                vocabularyRepository,
                userVocabularyRepository,
                userRepository,
                aiService,
                progressService,
                studySessionRepository);
    }

    @Test
    void reportsActualUnlearnedCountInsteadOfZeroOrOneSample() {
        UUID userId = UUID.randomUUID();
        when(vocabularyRepository.countUnlearnedByUser(eq(userId), any())).thenReturn(42L);

        long count = vocabularyService.countUnlearnedWords(userId, "BEGINNER");

        assertThat(count).isEqualTo(42);
    }

    @Test
    void capsNewWordsAtDailyLimit() {
        UUID userId = UUID.randomUUID();
        when(userVocabularyRepository.countByUserIdAndCreatedAtBetween(
                eq(userId),
                any(Instant.class),
                any(Instant.class))).thenReturn(13L);

        assertThat(vocabularyService.getRemainingNewWordsToday(userId)).isZero();
    }
}
