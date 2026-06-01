package com.englishai.progress.controller;

import com.englishai.common.enums.MetricType;
import com.englishai.progress.dto.ProgressHistoryDto;
import com.englishai.progress.service.ProgressService;
import com.englishai.user.entity.User;
import com.englishai.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/progress")
@RequiredArgsConstructor
@Slf4j
public class ProgressController {

    private final ProgressService progressService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private User getAuthenticatedUser(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("User is not authenticated");
        }
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + principal.getName()));
    }

    private int getXpThreshold(int level) {
        return switch (level) {
            case 1 -> 0;
            case 2 -> 100;
            case 3 -> 300;
            case 4 -> 600;
            case 5 -> 1000;
            case 6 -> 2000;
            case 7 -> 5000;
            default -> 5000;
        };
    }

    @GetMapping
    public String progress(Principal principal, Model model) {
        User user = getAuthenticatedUser(principal);
        model.addAttribute("user", user);
        model.addAttribute("from", LocalDate.now().minusDays(30));
        model.addAttribute("to", LocalDate.now());

        // True total XP
        int trueTotalXp = progressService.getTotalXp(user.getId());

        int currentLevel = progressService.calculateLevel(trueTotalXp);
        int currentLevelMinXp = getXpThreshold(currentLevel);
        int nextLevelXpThreshold = getXpThreshold(currentLevel + 1);

        int xpInCurrentLevel = trueTotalXp - currentLevelMinXp;
        int xpRequiredForNextLevel = nextLevelXpThreshold - currentLevelMinXp;
        int progressPercentage = (currentLevel == 7) ? 100 : (xpInCurrentLevel * 100) / xpRequiredForNextLevel;
        int xpToNextLevel = (currentLevel == 7) ? 0 : nextLevelXpThreshold - trueTotalXp;

        model.addAttribute("totalXp", trueTotalXp);
        model.addAttribute("currentLevel", currentLevel);
        model.addAttribute("progressPercentage", progressPercentage);
        model.addAttribute("xpToNextLevel", xpToNextLevel);
        model.addAttribute("nextLevelThreshold", nextLevelXpThreshold);
        model.addAttribute("metricTypes", MetricType.values());

        return "progress/index";
    }

    @GetMapping("/chart")
    public String progressChart(
            Principal principal,
            @RequestParam("metric") MetricType metric,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model) {

        User user = getAuthenticatedUser(principal);

        if (from == null) {
            from = LocalDate.now().minusDays(7);
        }
        if (to == null) {
            to = LocalDate.now();
        }

        ProgressHistoryDto history = progressService.getProgressHistory(user.getId(), metric, from, to);

        List<String> labels = new ArrayList<>();
        List<BigDecimal> data = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

        for (Map.Entry<LocalDate, BigDecimal> entry : history.getDailyValues().entrySet()) {
            labels.add(entry.getKey().format(formatter));
            data.add(entry.getValue());
        }

        Map<String, Object> chartData = new HashMap<>();
        chartData.put("labels", labels);

        Map<String, Object> dataset = new HashMap<>();
        String label = switch (metric) {
            case XP_TOTAL -> "Tổng điểm kinh nghiệm (XP)";
            case VOCABULARY_MASTERED -> "Từ vựng đã nắm vững";
            case EXERCISES_COMPLETED -> "Bài tập đã hoàn thành";
            case STREAK_DAYS -> "Chuỗi ngày liên tục";
            case LESSONS_COMPLETED -> "Bài học đã hoàn thành";
        };
        dataset.put("label", label);
        dataset.put("data", data);
        dataset.put("backgroundColor", "rgba(16, 185, 129, 0.1)"); // Emerald-500 light opacity
        dataset.put("borderColor", "rgba(16, 185, 129, 1)"); // Emerald-500
        dataset.put("borderWidth", 3);
        dataset.put("tension", 0.3); // Smooth curve
        dataset.put("fill", true);

        chartData.put("datasets", List.of(dataset));

        try {
            String chartJson = objectMapper.writeValueAsString(chartData);
            model.addAttribute("progressChartJson", chartJson);
            model.addAttribute("selectedMetric", metric);
        } catch (Exception e) {
            log.error("Error serializing progress history chart JSON", e);
            model.addAttribute("progressChartJson", "{}");
        }

        return "progress/fragments/progress-chart";
    }
}
