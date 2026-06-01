package com.englishai.dashboard.controller;

import com.englishai.dashboard.dto.DashboardSummaryDto;
import com.englishai.dashboard.dto.RecentActivityDto;
import com.englishai.dashboard.dto.WeakTopicDto;
import com.englishai.dashboard.dto.WeeklyProgressDto;
import com.englishai.dashboard.service.DashboardService;
import com.englishai.user.entity.User;
import com.englishai.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private User getAuthenticatedUser(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("User is not authenticated");
        }
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + principal.getName()));
    }

    @GetMapping
    public String dashboard(Principal principal, Model model) {
        User user = getAuthenticatedUser(principal);
        model.addAttribute("user", user);
        return "dashboard/index";
    }

    @GetMapping("/summary")
    public String summaryCards(Principal principal, Model model) {
        User user = getAuthenticatedUser(principal);
        DashboardSummaryDto summary = dashboardService.getSummary(user.getId());
        model.addAttribute("summary", summary);
        return "dashboard/fragments/summary-cards";
    }

    @GetMapping("/weekly-chart")
    public String weeklyChart(Principal principal, Model model) {
        User user = getAuthenticatedUser(principal);
        WeeklyProgressDto weeklyProgress = dashboardService.getWeeklyProgress(user.getId());

        List<String> labels = new ArrayList<>();
        List<Integer> data = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

        for (Map.Entry<LocalDate, Integer> entry : weeklyProgress.getMinutesPerDay().entrySet()) {
            labels.add(entry.getKey().format(formatter));
            data.add(entry.getValue());
        }

        Map<String, Object> chartData = new HashMap<>();
        chartData.put("labels", labels);

        Map<String, Object> dataset = new HashMap<>();
        dataset.put("label", "Minutes Studied");
        dataset.put("data", data);
        dataset.put("backgroundColor", "rgba(99, 102, 241, 0.2)"); // Indigo-500 with opacity
        dataset.put("borderColor", "rgba(99, 102, 241, 1)");
        dataset.put("borderWidth", 2);
        dataset.put("borderRadius", 6);

        chartData.put("datasets", List.of(dataset));

        try {
            String chartJson = objectMapper.writeValueAsString(chartData);
            model.addAttribute("weeklyChartJson", chartJson);
        } catch (Exception e) {
            log.error("Error serializing weekly progress chart JSON", e);
            model.addAttribute("weeklyChartJson", "{}");
        }

        return "dashboard/fragments/weekly-chart";
    }

    @GetMapping("/recent-activity")
    public String recentActivity(Principal principal, Model model) {
        User user = getAuthenticatedUser(principal);
        List<RecentActivityDto> recentActivities = dashboardService.getRecentActivity(user.getId(), 10);
        model.addAttribute("activities", recentActivities);
        return "dashboard/fragments/recent-activity";
    }

    @GetMapping("/weak-topics")
    public String weakTopics(Principal principal, Model model) {
        User user = getAuthenticatedUser(principal);
        List<WeakTopicDto> weakTopics = dashboardService.getWeakGrammarTopics(user.getId(), 5);
        model.addAttribute("weakTopics", weakTopics);
        return "dashboard/fragments/weak-topics";
    }
}
