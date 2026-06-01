package com.englishai.user.controller;

import com.englishai.user.dto.AdminPasswordResetRequest;
import com.englishai.user.dto.AdminUserCreateRequest;
import com.englishai.user.dto.AdminUserUpdateRequest;
import com.englishai.user.entity.Role;
import com.englishai.user.entity.User;
import com.englishai.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Slf4j
public class AdminUserController {

    private static final int PAGE_SIZE = 10;

    private final UserService userService;
    private final SessionRegistry sessionRegistry;

    @GetMapping
    public String index(@RequestParam(required = false) String keyword,
                        @RequestParam(required = false) Role role,
                        @RequestParam(required = false) Boolean enabled,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        model.addAttribute("users", userService.searchUsers(keyword, role, enabled, pageable));
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedRole", role);
        model.addAttribute("selectedEnabled", enabled);
        prepareFormModel(model);
        return "admin/users/index";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable UUID id,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Role role,
                       @RequestParam(required = false) Boolean enabled,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        User user = userService.getUserForAdmin(id);
        model.addAttribute("editingUser", user);
        model.addAttribute("updateRequest", new AdminUserUpdateRequest(
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled()
        ));
        model.addAttribute("resetPasswordRequest", new AdminPasswordResetRequest());
        return index(keyword, role, enabled, page, model);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("createRequest") AdminUserCreateRequest request,
                         BindingResult bindingResult,
                         @RequestParam(required = false) String keyword,
                         @RequestParam(required = false) Role role,
                         @RequestParam(required = false) Boolean enabled,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return indexWithSubmittedForm(keyword, role, enabled, model);
        }

        try {
            userService.createUser(request);
            redirectAttributes.addFlashAttribute("successMessage", "Đã tạo người dùng mới.");
        } catch (IllegalArgumentException e) {
            bindingResult.reject("create.error", e.getMessage());
            return indexWithSubmittedForm(keyword, role, enabled, model);
        } catch (Exception e) {
            log.error("Unable to create user {}", request.getEmail(), e);
            bindingResult.reject("create.error", "Không thể tạo người dùng. Vui lòng thử lại.");
            return indexWithSubmittedForm(keyword, role, enabled, model);
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable UUID id,
                         @Valid @ModelAttribute("updateRequest") AdminUserUpdateRequest request,
                         BindingResult bindingResult,
                         Principal principal,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        User editingUser = userService.getUserForAdmin(id);
        model.addAttribute("editingUser", editingUser);
        model.addAttribute("resetPasswordRequest", new AdminPasswordResetRequest());

        if (bindingResult.hasErrors()) {
            return index(null, null, null, 0, model);
        }

        try {
            boolean shouldExpireSessions = !editingUser.getEmail().equalsIgnoreCase(request.getEmail())
                    || editingUser.getRole() != request.getRole()
                    || !request.isEnabled();
            String previousEmail = editingUser.getEmail();

            userService.updateUser(id, request, principal.getName());
            if (shouldExpireSessions) {
                expireSessions(previousEmail);
            }
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật người dùng.");
        } catch (IllegalArgumentException e) {
            bindingResult.reject("update.error", e.getMessage());
            return index(null, null, null, 0, model);
        } catch (Exception e) {
            log.error("Unable to update user {}", id, e);
            bindingResult.reject("update.error", "Không thể cập nhật người dùng. Vui lòng thử lại.");
            return index(null, null, null, 0, model);
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/password")
    public String resetPassword(@PathVariable UUID id,
                                @Valid @ModelAttribute("resetPasswordRequest") AdminPasswordResetRequest request,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        User editingUser = userService.getUserForAdmin(id);
        model.addAttribute("editingUser", editingUser);
        model.addAttribute("updateRequest", new AdminUserUpdateRequest(
                editingUser.getFullName(),
                editingUser.getEmail(),
                editingUser.getRole(),
                editingUser.isEnabled()
        ));

        if (bindingResult.hasErrors()) {
            return index(null, null, null, 0, model);
        }

        userService.resetPassword(id, request);
        redirectAttributes.addFlashAttribute("successMessage", "Đã đặt lại mật khẩu.");
        return "redirect:/admin/users/" + id + "/edit";
    }

    @PostMapping("/{id}/toggle-enabled")
    public String toggleEnabled(@PathVariable UUID id,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getUserForAdmin(id);
            boolean wasEnabled = user.isEnabled();
            String email = user.getEmail();

            userService.toggleEnabled(id, principal.getName());
            if (wasEnabled) {
                expireSessions(email);
            }
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái tài khoản.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable UUID id,
                         Principal principal,
                         RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getUserForAdmin(id);
            String email = user.getEmail();

            userService.deleteUser(id, principal.getName());
            expireSessions(email);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa người dùng.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    private String indexWithSubmittedForm(String keyword, Role role, Boolean enabled, Model model) {
        prepareFormModel(model);
        return index(keyword, role, enabled, 0, model);
    }

    private void prepareFormModel(Model model) {
        if (!model.containsAttribute("createRequest")) {
            model.addAttribute("createRequest", new AdminUserCreateRequest());
        }
        if (!model.containsAttribute("updateRequest")) {
            model.addAttribute("updateRequest", new AdminUserUpdateRequest());
        }
        if (!model.containsAttribute("resetPasswordRequest")) {
            model.addAttribute("resetPasswordRequest", new AdminPasswordResetRequest());
        }
        model.addAttribute("roles", Role.values());
    }

    private void expireSessions(String email) {
        sessionRegistry.getAllPrincipals().stream()
                .filter(principal -> email.equalsIgnoreCase(principalName(principal)))
                .flatMap(principal -> sessionRegistry.getAllSessions(principal, false).stream())
                .forEach(SessionInformation::expireNow);
    }

    private String principalName(Object principal) {
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof Principal namedPrincipal) {
            return namedPrincipal.getName();
        }
        return String.valueOf(principal);
    }
}
