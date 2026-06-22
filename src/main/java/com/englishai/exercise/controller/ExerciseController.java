package com.englishai.exercise.controller;

import com.englishai.common.enums.ExerciseType;
import com.englishai.common.enums.Level;
import com.englishai.common.enums.AttemptStatus;
import com.englishai.exercise.dto.*;
import com.englishai.exercise.service.ExerciseService;
import com.englishai.exercise.service.LearnerLevelService;
import com.englishai.user.entity.User;
import com.englishai.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import com.englishai.exercise.dto.ExerciseFilter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/exercises")
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseService exerciseService;
    private final LearnerLevelService learnerLevelService;
    private final UserRepository userRepository;

    @GetMapping
    public String index(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "exerciseType", required = false) ExerciseType exerciseType,
            @RequestParam(value = "difficulty", required = false) Level difficulty,
            @PageableDefault(size = 12) Pageable pageable,
            Principal principal,
            Model model) {
        User user = getAuthenticatedUser(principal);
        Level effectiveDifficulty = difficulty != null
                ? difficulty
                : learnerLevelService.determineLevel(user.getId());
        ExerciseFilter filter = new ExerciseFilter(keyword, exerciseType, effectiveDifficulty);
        Page<ExerciseDto> exercises = exerciseService.getPublishedExercises(filter, pageable);
        model.addAttribute("exercises", exercises);
        model.addAttribute("filter", filter);
        model.addAttribute("exerciseTypes", ExerciseType.values());
        model.addAttribute("levels", Level.values());
        model.addAttribute("adaptiveSelection", difficulty == null);
        model.addAttribute("listBaseUrl", buildListBaseUrl(keyword, exerciseType, effectiveDifficulty));
        return "exercises/index";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        model.addAttribute("detail", exerciseService.getExerciseWithQuestions(id));
        return "exercises/detail";
    }

    @PostMapping("/{id}/start")
    public String start(@PathVariable UUID id, Principal principal) {
        User user = getAuthenticatedUser(principal);
        ExerciseAttemptDto attempt = exerciseService.startAttempt(user.getId(), id);
        return "redirect:/exercises/attempt/" + attempt.id();
    }

    @GetMapping("/attempt/{attemptId}")
    public String attempt(@PathVariable UUID attemptId, Principal principal, Model model) {
        User user = getAuthenticatedUser(principal);
        ExerciseAttemptDto attempt = exerciseService.getAttempt(user.getId(), attemptId);
        if (attempt.status() == AttemptStatus.COMPLETED) {
            return "redirect:/exercises/attempt/" + attemptId + "/result";
        }
        model.addAttribute("attempt", attempt);
        model.addAttribute("detail", exerciseService.getExerciseWithQuestions(attempt.exerciseId()));
        return "exercises/attempt";
    }

    @PostMapping("/attempt/{attemptId}/submit")
    public String complete(
            @PathVariable UUID attemptId,
            @RequestParam Map<String, String> requestParameters,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(principal);
        try {
            exerciseService.submitAttempt(user.getId(), attemptId, extractAnswers(requestParameters));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/exercises/attempt/" + attemptId;
        }
        return "redirect:/exercises/attempt/" + attemptId + "/result";
    }

    @GetMapping("/attempt/{attemptId}/result")
    public String result(@PathVariable UUID attemptId, Principal principal, Model model) {
        User user = getAuthenticatedUser(principal);
        model.addAttribute("result", exerciseService.getAttemptResult(user.getId(), attemptId));
        return "exercises/result";
    }

    @GetMapping("/generate")
    public String generateForm(Principal principal, Model model) {
        User user = getAuthenticatedUser(principal);
        model.addAttribute("generateExerciseRequest", new GenerateExerciseRequest());
        model.addAttribute("exerciseTypes", ExerciseType.values());
        model.addAttribute("learnerLevel", learnerLevelService.determineLevel(user.getId()));
        return "exercises/generate";
    }

    @PostMapping("/generate")
    public String generate(
            @Valid @ModelAttribute("generateExerciseRequest") GenerateExerciseRequest request,
            BindingResult bindingResult,
            Principal principal,
            Model model,
            HttpServletResponse response,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest) {
        model.addAttribute("exerciseTypes", ExerciseType.values());
        User user = getAuthenticatedUser(principal);
        model.addAttribute("learnerLevel", learnerLevelService.determineLevel(user.getId()));
        if (bindingResult.hasErrors()) {
            if (hxRequest != null) {
                StringBuilder errorMsg = new StringBuilder();
                bindingResult.getFieldErrors().forEach(e ->
                        errorMsg.append(e.getDefaultMessage()).append(". "));
                model.addAttribute("errorMessage", errorMsg.toString().trim());
                return "exercises/fragments/generate-error";
            }
            return "exercises/generate";
        }

        ExerciseDto exercise;
        try {
            exercise = exerciseService.generateAiExercise(user.getId(), request);
        } catch (Exception e) {
            if (hxRequest != null) {
                model.addAttribute("errorMessage", "Lỗi tạo bài tập: " + e.getMessage());
                return "exercises/fragments/generate-error";
            }
            model.addAttribute("errorMessage", "Lỗi tạo bài tập: " + e.getMessage());
            return "exercises/generate";
        }
        String redirectUrl = "/exercises/" + exercise.id();
        if (hxRequest != null) {
            response.setHeader("HX-Redirect", redirectUrl);
            return "exercises/fragments/generate-progress";
        }
        return "redirect:" + redirectUrl;
    }

    private User getAuthenticatedUser(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("User is not authenticated");
        }
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + principal.getName()));
    }

    private String buildListBaseUrl(String keyword, ExerciseType exerciseType, Level difficulty) {
        StringBuilder url = new StringBuilder("/exercises");
        String separator = "?";
        if (keyword != null && !keyword.isBlank()) {
            url.append(separator).append("keyword=").append(keyword.trim());
            separator = "&";
        }
        if (exerciseType != null) {
            url.append(separator).append("exerciseType=").append(exerciseType.name());
            separator = "&";
        }
        if (difficulty != null) {
            url.append(separator).append("difficulty=").append(difficulty.name());
        }
        return url.toString();
    }

    private Map<UUID, String> extractAnswers(Map<String, String> requestParameters) {
        Map<UUID, String> answers = new HashMap<>();
        requestParameters.forEach((name, value) -> {
            if (name.startsWith("answer_")) {
                answers.put(UUID.fromString(name.substring("answer_".length())), value);
            }
        });
        return answers;
    }
}
