package com.englishai.vocabulary.controller;

import com.englishai.ai.dto.VocabularyInfoDto;
import com.englishai.common.enums.Level;
import com.englishai.user.entity.User;
import com.englishai.user.repository.UserRepository;
import com.englishai.vocabulary.dto.UserVocabularyDto;
import com.englishai.vocabulary.dto.VocabularyDto;
import com.englishai.vocabulary.dto.VocabularyStatsDto;
import com.englishai.vocabulary.service.VocabularyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/vocabulary")
@RequiredArgsConstructor
public class VocabularyController {

    private final VocabularyService vocabularyService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @GetMapping
    public String index(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "level", required = false) String level,
            @PageableDefault(size = 12) Pageable pageable,
            Model model) {
        Page<VocabularyDto> vocabularies = vocabularyService.searchVocabularies(keyword, level, pageable);
        model.addAttribute("vocabularies", vocabularies);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedLevel", level);
        model.addAttribute("levels", Level.values());
        model.addAttribute("listBaseUrl", buildListBaseUrl(keyword, level));
        return "vocabulary/index";
    }

    @GetMapping("/list")
    public String list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "level", required = false) String level,
            @PageableDefault(size = 12) Pageable pageable,
            Model model) {
        Page<VocabularyDto> vocabularies = vocabularyService.searchVocabularies(keyword, level, pageable);
        model.addAttribute("vocabularies", vocabularies);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedLevel", level);
        model.addAttribute("listBaseUrl", buildListBaseUrl(keyword, level));
        return "vocabulary/fragments/vocabulary-list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        model.addAttribute("vocabulary", vocabularyService.getById(id));
        return "vocabulary/fragments/vocabulary-card :: detail";
    }

    @PostMapping("/{id}/add")
    public String addToUserList(@PathVariable UUID id, Principal principal, Model model) {
        User user = getAuthenticatedUser(principal);
        UserVocabularyDto userVocabulary = vocabularyService.addToUserList(user.getId(), id);
        model.addAttribute("userVocabulary", userVocabulary);
        model.addAttribute("vocabulary", vocabularyService.getById(id));
        model.addAttribute("added", true);
        return "vocabulary/fragments/vocabulary-card :: detail";
    }

    @GetMapping("/learn")
    public String learn(Principal principal, Model model) {
        User user = getAuthenticatedUser(principal);
        Map<String, Long> levelCounts = new LinkedHashMap<>();
        for (Level lv : Level.values()) {
            List<VocabularyDto> words = vocabularyService.getUnlearnedWords(user.getId(), lv.name(), 1);
            levelCounts.put(lv.name(), (long) words.size());
        }
        model.addAttribute("levelCounts", levelCounts);
        return "vocabulary/learn";
    }

    @GetMapping("/learn/session")
    public String learnSession(
            @RequestParam(value = "level", required = false) String level,
            Principal principal, Model model) {
        User user = getAuthenticatedUser(principal);
        List<VocabularyDto> words = vocabularyService.getUnlearnedWords(user.getId(), level, 1);
        if (!words.isEmpty()) {
            vocabularyService.addToUserList(user.getId(), UUID.fromString(words.get(0).getId()));
        }
        model.addAttribute("word", words.isEmpty() ? null : words.get(0));
        model.addAttribute("level", level);
        return "vocabulary/fragments/learn-card";
    }

    @GetMapping("/review")
    public String review(Principal principal, Model model) {
        User user = getAuthenticatedUser(principal);
        model.addAttribute("dueCount", vocabularyService.getDueForReview(user.getId(), 100).size());
        return "vocabulary/review";
    }

    @GetMapping("/review/next")
    public String nextReview(Principal principal, Model model) {
        User user = getAuthenticatedUser(principal);
        List<UserVocabularyDto> due = vocabularyService.getDueForReview(user.getId(), 1);
        model.addAttribute("userVocabulary", due.isEmpty() ? null : due.get(0));
        return "vocabulary/fragments/review-card";
    }

    @GetMapping("/review/random")
    public String randomReview(Principal principal, Model model) {
        return "vocabulary/random-review";
    }

    @GetMapping("/review/random/next")
    public String randomReviewNext(
            @RequestParam(value = "level", required = false) String level,
            Principal principal, Model model) {
        User user = getAuthenticatedUser(principal);
        List<UserVocabularyDto> words = vocabularyService.getRandomForReview(user.getId(), level, 1);
        model.addAttribute("userVocabulary", words.isEmpty() ? null : words.get(0));
        model.addAttribute("level", level);
        return "vocabulary/fragments/review-card";
    }

    @PostMapping("/review/{id}/submit")
    public String submitReview(
            @PathVariable UUID id,
            @RequestParam("quality") int quality,
            @RequestParam(value = "level", required = false) String level,
            Principal principal,
            Model model) {
        User user = getAuthenticatedUser(principal);
        vocabularyService.submitReview(user.getId(), id, quality);
        // Load next word directly
        List<UserVocabularyDto> next;
        if (level != null && !level.isBlank()) {
            next = vocabularyService.getRandomForReview(user.getId(), level, 1);
        } else {
            next = vocabularyService.getDueForReview(user.getId(), 1);
        }
        model.addAttribute("userVocabulary", next.isEmpty() ? null : next.get(0));
        model.addAttribute("level", level);
        return "vocabulary/fragments/review-card";
    }

    @GetMapping("/levels")
    public String levels(
            @RequestParam(value = "level", required = false, defaultValue = "BEGINNER") String level,
            @PageableDefault(size = 12) Pageable pageable,
            Model model) {
        Level selectedLevel;
        try {
            selectedLevel = Level.valueOf(level.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            selectedLevel = Level.BEGINNER;
        }

        Page<VocabularyDto> vocabularies = vocabularyService.getByLevel(selectedLevel, pageable);
        model.addAttribute("vocabularies", vocabularies);
        model.addAttribute("selectedLevel", selectedLevel.name());
        model.addAttribute("levels", Level.values());
        model.addAttribute("levelCounts", vocabularyService.getLevelCounts());
        model.addAttribute("listBaseUrl", "/vocabulary/levels/list?level=" + encode(selectedLevel.name()));
        return "vocabulary/levels";
    }

    @GetMapping("/levels/list")
    public String levelsList(
            @RequestParam(value = "level", required = false, defaultValue = "BEGINNER") String level,
            @PageableDefault(size = 12) Pageable pageable,
            Model model) {
        Level selectedLevel;
        try {
            selectedLevel = Level.valueOf(level.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            selectedLevel = Level.BEGINNER;
        }

        Page<VocabularyDto> vocabularies = vocabularyService.getByLevel(selectedLevel, pageable);
        model.addAttribute("vocabularies", vocabularies);
        model.addAttribute("listBaseUrl", "/vocabulary/levels/list?level=" + encode(selectedLevel.name()));
        return "vocabulary/fragments/vocabulary-list";
    }

    @GetMapping("/stats")
    public String stats(Principal principal, Model model) {
        User user = getAuthenticatedUser(principal);
        model.addAttribute("stats", vocabularyService.getMasteryStats(user.getId()));
        return "vocabulary/fragments/stats-widget";
    }

    @GetMapping("/statistics")
    public String statistics(Principal principal, Model model) {
        User user = getAuthenticatedUser(principal);
        VocabularyStatsDto stats = vocabularyService.getDetailedStats(user.getId());
        model.addAttribute("stats", stats);

        try {
            // Mastery distribution chart data
            Map<String, Object> masteryChartData = new LinkedHashMap<>();
            masteryChartData.put("labels", stats.getMasteryDistribution().keySet());
            Map<String, Object> masteryDataset = new LinkedHashMap<>();
            masteryDataset.put("data", stats.getMasteryDistribution().values());
            masteryDataset.put("backgroundColor", List.of(
                    "#6c63ff", "#10b981", "#f59e0b", "#ef4444", "#3b82f6", "#8b5cf6"));
            masteryChartData.put("datasets", List.of(masteryDataset));
            model.addAttribute("masteryChartJson", objectMapper.writeValueAsString(masteryChartData));

            // Words added over time chart data
            Map<String, Object> wordsAddedData = new LinkedHashMap<>();
            List<String> dateLabels = stats.getWordsAddedOverTime().keySet().stream()
                    .map(d -> d.format(DateTimeFormatter.ofPattern("dd/MM")))
                    .toList();
            wordsAddedData.put("labels", dateLabels);
            Map<String, Object> wordsAddedDataset = new LinkedHashMap<>();
            wordsAddedDataset.put("label", "Từ vựng đã thêm");
            wordsAddedDataset.put("data", List.copyOf(stats.getWordsAddedOverTime().values()));
            wordsAddedDataset.put("borderColor", "#6c63ff");
            wordsAddedDataset.put("backgroundColor", "rgba(108, 99, 255, 0.1)");
            wordsAddedDataset.put("fill", true);
            wordsAddedDataset.put("tension", 0.3);
            wordsAddedDataset.put("borderWidth", 2);
            wordsAddedData.put("datasets", List.of(wordsAddedDataset));
            model.addAttribute("wordsAddedChartJson", objectMapper.writeValueAsString(wordsAddedData));

            // Reviews over time chart data
            Map<String, Object> reviewsData = new LinkedHashMap<>();
            reviewsData.put("labels", dateLabels);
            Map<String, Object> reviewsDataset = new LinkedHashMap<>();
            reviewsDataset.put("label", "Lượt ôn tập");
            reviewsDataset.put("data", List.copyOf(stats.getReviewsOverTime().values()));
            reviewsDataset.put("borderColor", "#10b981");
            reviewsDataset.put("backgroundColor", "rgba(16, 185, 129, 0.1)");
            reviewsDataset.put("fill", true);
            reviewsDataset.put("tension", 0.3);
            reviewsDataset.put("borderWidth", 2);
            reviewsData.put("datasets", List.of(reviewsDataset));
            model.addAttribute("reviewsChartJson", objectMapper.writeValueAsString(reviewsData));
        } catch (Exception e) {
            model.addAttribute("masteryChartJson", "{}");
            model.addAttribute("wordsAddedChartJson", "{}");
            model.addAttribute("reviewsChartJson", "{}");
        }

        return "vocabulary/statistics";
    }

    @GetMapping("/{id}/ai-explanation")
    public String aiExplanation(@PathVariable UUID id, Principal principal, Model model) {
        User user = getAuthenticatedUser(principal);
        VocabularyInfoDto explanation = vocabularyService.getAiExplanation(user.getId(), id);
        model.addAttribute("explanation", explanation);
        return "vocabulary/fragments/vocabulary-card :: aiExplanation";
    }

    private User getAuthenticatedUser(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("User is not authenticated");
        }
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + principal.getName()));
    }

    private String buildListBaseUrl(String keyword, String level) {
        StringBuilder url = new StringBuilder("/vocabulary/list");
        String separator = "?";
        if (keyword != null && !keyword.isBlank()) {
            url.append(separator).append("keyword=").append(encode(keyword.trim()));
            separator = "&";
        }
        if (level != null && !level.isBlank()) {
            url.append(separator).append("level=").append(encode(level.trim()));
        }
        return url.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
