package com.englishai.grammar.controller;

import com.englishai.ai.dto.GrammarCorrectionResult;
import com.englishai.ai.dto.GrammarExplanationDto;
import com.englishai.common.enums.Level;
import com.englishai.grammar.service.GrammarService;
import com.englishai.user.entity.User;
import com.englishai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequestMapping("/grammar")
@RequiredArgsConstructor
public class GrammarController {

    private final GrammarService grammarService;
    private final UserRepository userRepository;

    @GetMapping
    public String index(
            @RequestParam(value = "level", required = false) Level level,
            Model model) {
        model.addAttribute("topics", grammarService.getPublishedTopics(level));
        model.addAttribute("levels", Level.values());
        model.addAttribute("selectedLevel", level);
        return "grammar/index";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        model.addAttribute("topic", grammarService.getPublishedTopic(id));
        return "grammar/detail";
    }

    @PostMapping("/check")
    public String checkGrammar(
            @RequestParam("text") String text,
            Principal principal,
            Model model) {
        try {
            User user = getAuthenticatedUser(principal);
            GrammarCorrectionResult result = grammarService.checkGrammar(user.getId(), text);
            model.addAttribute("result", result);
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Không thể kiểm tra ngữ pháp lúc này: " + e.getMessage());
        }
        return "grammar/fragments/check-result";
    }

    @GetMapping("/{id}/ai-explanation")
    public String aiExplanation(@PathVariable UUID id, Principal principal, Model model) {
        try {
            User user = getAuthenticatedUser(principal);
            var topic = grammarService.getPublishedTopic(id);
            GrammarExplanationDto explanation = grammarService.explainWithAi(user.getId(), topic.title(), topic.level());
            model.addAttribute("explanation", explanation);
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Không thể tạo giải thích AI lúc này: " + e.getMessage());
        }
        return "grammar/fragments/ai-explanation";
    }

    private User getAuthenticatedUser(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("User is not authenticated");
        }
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + principal.getName()));
    }
}
