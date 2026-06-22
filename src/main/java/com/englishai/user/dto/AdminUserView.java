package com.englishai.user.dto;

import com.englishai.exercise.dto.LearnerProfileDto;
import com.englishai.user.entity.Role;
import com.englishai.user.entity.User;

import java.time.Instant;
import java.util.UUID;

public record AdminUserView(
        UUID id,
        String fullName,
        String email,
        Role role,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt,
        LearnerProfileDto learnerProfile
) {
    public static AdminUserView from(User user, LearnerProfileDto learnerProfile) {
        return new AdminUserView(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                learnerProfile
        );
    }

    public String roleLabel() {
        return role == Role.ADMIN ? "Quản trị" : "Học viên";
    }

    public boolean hasLearnerProfile() {
        return role == Role.USER && learnerProfile != null;
    }
}
