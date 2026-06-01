package com.englishai.user.service;

import com.englishai.auth.dto.RegisterRequest;
import com.englishai.user.dto.PasswordChangeRequest;
import com.englishai.user.dto.ProfileUpdateRequest;
import com.englishai.user.entity.Role;
import com.englishai.user.entity.User;
import com.englishai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Business logic for user account management.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registers a new user from the registration form.
     *
     * @param request validated registration DTO
     * @return the persisted {@link User} entity
     * @throws IllegalArgumentException if the email is already taken
     */
    @Transactional
    public User registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email đã được sử dụng: " + request.getEmail());
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
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
        user.setFullName(request.getFullName());
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
}
