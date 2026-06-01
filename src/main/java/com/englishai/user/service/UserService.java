package com.englishai.user.service;

import com.englishai.auth.dto.RegisterRequest;
import com.englishai.user.dto.AdminPasswordResetRequest;
import com.englishai.user.dto.AdminUserCreateRequest;
import com.englishai.user.dto.AdminUserUpdateRequest;
import com.englishai.user.dto.AdminUserView;
import com.englishai.user.dto.PasswordChangeRequest;
import com.englishai.user.dto.ProfileUpdateRequest;
import com.englishai.user.entity.Role;
import com.englishai.user.entity.User;
import com.englishai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * Business logic for user account management.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<AdminUserView> searchUsers(String keyword, Role role, Boolean enabled, Pageable pageable) {
        Specification<User> spec = Specification.where(null);

        if (StringUtils.hasText(keyword)) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern)
            ));
        }

        if (role != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("role"), role));
        }

        if (enabled != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("enabled"), enabled));
        }

        return userRepository.findAll(spec, pageable).map(AdminUserView::from);
    }

    @Transactional(readOnly = true)
    public User getUserForAdmin(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));
    }

    @Transactional
    public User createUser(AdminUserCreateRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email đã được sử dụng.");
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .enabled(request.isEnabled())
                .build();

        return userRepository.saveAndFlush(user);
    }

    @Transactional
    public User updateUser(UUID userId, AdminUserUpdateRequest request, String currentAdminEmail) {
        User user = getUserForAdmin(userId);
        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmailAndIdNot(email, userId)) {
            throw new IllegalArgumentException("Email đã được sử dụng.");
        }

        boolean updatingSelf = user.getEmail().equalsIgnoreCase(currentAdminEmail);
        if (updatingSelf && !email.equalsIgnoreCase(user.getEmail())) {
            throw new IllegalArgumentException("Không thể đổi email của tài khoản admin đang đăng nhập.");
        }
        if (updatingSelf && (request.getRole() != Role.ADMIN || !request.isEnabled())) {
            throw new IllegalArgumentException("Không thể tự hạ quyền hoặc khóa tài khoản admin đang đăng nhập.");
        }

        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setRole(request.getRole());
        user.setEnabled(request.isEnabled());

        return userRepository.saveAndFlush(user);
    }

    @Transactional
    public void resetPassword(UUID userId, AdminPasswordResetRequest request) {
        User user = getUserForAdmin(userId);
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.saveAndFlush(user);
    }

    @Transactional
    public void toggleEnabled(UUID userId, String currentAdminEmail) {
        User user = getUserForAdmin(userId);
        if (user.getEmail().equalsIgnoreCase(currentAdminEmail)) {
            throw new IllegalArgumentException("Không thể khóa tài khoản admin đang đăng nhập.");
        }

        user.setEnabled(!user.isEnabled());
        userRepository.saveAndFlush(user);
    }

    @Transactional
    public void deleteUser(UUID userId, String currentAdminEmail) {
        User user = getUserForAdmin(userId);
        if (user.getEmail().equalsIgnoreCase(currentAdminEmail)) {
            throw new IllegalArgumentException("Không thể xóa tài khoản admin đang đăng nhập.");
        }

        userRepository.delete(user);
        userRepository.flush();
    }

    /**
     * Registers a new user from the registration form.
     *
     * @param request validated registration DTO
     * @return the persisted {@link User} entity
     * @throws IllegalArgumentException if the email is already taken
     */
    @Transactional
    public User registerUser(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email đã được sử dụng: " + email);
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .enabled(true)
                .build();

        return userRepository.saveAndFlush(user);
    }

    /**
     * Updates the user's basic profile details (Full Name).
     */
    @Transactional
    public User updateProfile(UUID userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + userId));
        user.setFullName(request.getFullName().trim());
        return userRepository.saveAndFlush(user);
    }

    /**
     * Changes the user's password after verifying their current password.
     */
    @Transactional
    public void changePassword(UUID userId, PasswordChangeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.saveAndFlush(user);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
