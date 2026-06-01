package com.englishai.conversation.controller;

import com.englishai.ai.exception.AiRateLimitExceededException;
import com.englishai.ai.exception.OpenAiException;
import com.englishai.conversation.dto.ConversationDetailDto;
import com.englishai.conversation.dto.ConversationDto;
import com.englishai.conversation.dto.MessageResponseDto;
import com.englishai.conversation.service.ConversationService;
import com.englishai.user.entity.User;
import com.englishai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/conversation")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final UserRepository userRepository;

    @GetMapping
    public String index(
            Principal principal,
            @PageableDefault(size = 12) Pageable pageable,
            Model model) {
        User user = getAuthenticatedUser(principal);
        Page<ConversationDto> conversations = conversationService.getUserConversations(user.getId(), pageable);
        model.addAttribute("conversations", conversations);
        return "conversation/index";
    }

    @PostMapping("/new")
    public String create(
            Principal principal,
            @RequestParam(value = "title", required = false) String title) {
        User user = getAuthenticatedUser(principal);
        ConversationDto conversation = conversationService.createConversation(user.getId(), title);
        return "redirect:/conversation/" + conversation.getId();
    }

    @PostMapping("/start")
    public String start(
            Principal principal,
            @RequestParam("message") String message) {
        User user = getAuthenticatedUser(principal);
        String title = buildTitleFromMessage(message);
        ConversationDto conversation = conversationService.createConversation(user.getId(), title);
        conversationService.sendMessage(user.getId(), conversation.getId(), message);
        return "redirect:/conversation/" + conversation.getId();
    }

    @GetMapping("/{id}")
    public String chat(@PathVariable UUID id, Principal principal, Model model) {
        User user = getAuthenticatedUser(principal);
        ConversationDetailDto conversation;
        try {
            conversation = conversationService.getConversationWithMessages(id, user.getId());
        } catch (IllegalArgumentException ex) {
            log.warn("Conversation not found: {}", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuộc hội thoại không tồn tại");
        } catch (AccessDeniedException ex) {
            log.warn("Access denied for conversation {} by user {}", id, user.getId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập cuộc hội thoại này");
        } catch (Exception ex) {
            log.error("Error loading conversation {}", id, ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi tải cuộc hội thoại");
        }
        log.debug("Loaded conversation {} with {} messages", id,
                conversation.getMessages() != null ? conversation.getMessages().size() : 0);
        model.addAttribute("conversation", conversation);
        return "conversation/chat";
    }

    @PostMapping("/{id}/message")
    public String sendMessage(
            @PathVariable UUID id,
            @RequestParam("message") String message,
            Principal principal,
            Model model,
            jakarta.servlet.http.HttpServletResponse response) {
        try {
            User user = getAuthenticatedUser(principal);
            MessageResponseDto responseDto = conversationService.sendMessage(user.getId(), id, message);
            model.addAttribute("chatMsg", responseDto);

            // Set the HTMX trigger header to update the title on the client side
            if (responseDto.getConversationTitle() != null) {
                String safeTitle = responseDto.getConversationTitle()
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"");
                response.setHeader("HX-Trigger", "{\"conversationTitleUpdated\": \"" + safeTitle + "\"}");
            }

            return "conversation/fragments/message-bubble :: bubble(msg=${chatMsg})";
        } catch (IllegalArgumentException ex) {
            log.warn("Bad request in sendMessage: {}", ex.getMessage());
            model.addAttribute("message", ex.getMessage());
            return "conversation/fragments/error-bubble :: error";
        } catch (AccessDeniedException ex) {
            log.warn("Access denied in sendMessage for conversation {}", id);
            model.addAttribute("message", "Bạn không có quyền truy cập cuộc hội thoại này.");
            return "conversation/fragments/error-bubble :: error";
        } catch (AiRateLimitExceededException ex) {
            log.warn("Rate limit exceeded in sendMessage: {}", ex.getMessage());
            model.addAttribute("message", ex.getMessage());
            return "conversation/fragments/error-bubble :: error";
        } catch (OpenAiException ex) {
            log.error("OpenAI error in sendMessage for conversation {}", id, ex);
            model.addAttribute("message", "Dịch vụ AI tạm thời không khả dụng. Vui lòng thử lại sau.");
            return "conversation/fragments/error-bubble :: error";
        } catch (Exception ex) {
            log.error("Unexpected error in sendMessage for conversation {}", id, ex);
            model.addAttribute("message", "Đã xảy ra lỗi không mong muốn. Vui lòng thử lại.");
            return "conversation/fragments/error-bubble :: error";
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archive(@PathVariable UUID id, Principal principal) {
        User user = getAuthenticatedUser(principal);
        conversationService.archiveConversation(id, user.getId());
        return ResponseEntity.noContent().header("HX-Redirect", "/conversation").build();
    }

    @GetMapping("/{id}/messages")
    public String messages(
            @PathVariable UUID id,
            Principal principal,
            @PageableDefault(size = 20) Pageable pageable,
            Model model) {
        User user = getAuthenticatedUser(principal);
        List<MessageResponseDto> messages = conversationService.getMessages(id, user.getId(), pageable);
        model.addAttribute("messages", messages);
        return "conversation/fragments/message-list";
    }

    private User getAuthenticatedUser(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("User is not authenticated");
        }
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + principal.getName()));
    }

    private String buildTitleFromMessage(String message) {
        if (message == null || message.isBlank()) {
            return "New English chat";
        }
        String compactMessage = message.trim().replaceAll("\\s+", " ");
        return compactMessage.length() > 64 ? compactMessage.substring(0, 61) + "..." : compactMessage;
    }
}
