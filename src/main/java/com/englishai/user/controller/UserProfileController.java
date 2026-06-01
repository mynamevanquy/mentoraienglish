package com.englishai.user.controller;

import com.englishai.user.dto.PasswordChangeRequest;
import com.englishai.user.dto.ProfileUpdateRequest;
import com.englishai.user.entity.User;
import com.englishai.user.repository.UserRepository;
import com.englishai.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * Controller for managing user profile and account settings.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {

    private final UserService userService;
    private final UserRepository userRepository;

    private User getAuthenticatedUser(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("User is not authenticated");
        }
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + principal.getName()));
    }

    // -------------------------------------------------------------------------
    // Profile Page
    // -------------------------------------------------------------------------

    @GetMapping("/profile")
    public String profilePage(Principal principal, Model model) {
        User user = getAuthenticatedUser(principal);
        model.addAttribute("user", user);
        
        // Populate profileRequest with the current full name
        if (!model.containsAttribute("profileRequest")) {
            model.addAttribute("profileRequest", new ProfileUpdateRequest(user.getFullName()));
        }
        
        return "user/profile";
    }

    @PostMapping("/profile")
    public String handleUpdateProfile(
            @Valid @ModelAttribute("profileRequest") ProfileUpdateRequest request,
            BindingResult bindingResult,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        User user = getAuthenticatedUser(principal);

        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            return "user/profile";
        }

        try {
            userService.updateProfile(user.getId(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật họ và tên thành công!");
        } catch (Exception e) {
            log.error("Error updating profile for user {}", user.getEmail(), e);
            bindingResult.reject("profile.error", "Có lỗi xảy ra khi cập nhật thông tin. Vui lòng thử lại!");
            model.addAttribute("user", user);
            return "user/profile";
        }

        return "redirect:/profile";
    }

    // -------------------------------------------------------------------------
    // Settings Page (Change Password)
    // -------------------------------------------------------------------------

    @GetMapping("/settings")
    public String settingsPage(Principal principal, Model model) {
        User user = getAuthenticatedUser(principal);
        model.addAttribute("user", user);
        
        if (!model.containsAttribute("passwordRequest")) {
            model.addAttribute("passwordRequest", new PasswordChangeRequest());
        }
        
        return "user/settings";
    }

    @PostMapping("/settings/change-password")
    public String handleChangePassword(
            @Valid @ModelAttribute("passwordRequest") PasswordChangeRequest request,
            BindingResult bindingResult,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        User user = getAuthenticatedUser(principal);

        // Validation 1: check standard constraints
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            return "user/settings";
        }

        // Validation 2: check matching password
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Mật khẩu xác nhận không khớp");
            model.addAttribute("user", user);
            return "user/settings";
        }

        try {
            userService.changePassword(user.getId(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công!");
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("currentPassword", "password.invalid", e.getMessage());
            model.addAttribute("user", user);
            return "user/settings";
        } catch (Exception e) {
            log.error("Error changing password for user {}", user.getEmail(), e);
            bindingResult.reject("password.error", "Có lỗi xảy ra khi đổi mật khẩu. Vui lòng thử lại!");
            model.addAttribute("user", user);
            return "user/settings";
        }

        return "redirect:/settings";
    }
}
