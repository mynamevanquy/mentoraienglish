package com.englishai.user.dto;

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
        Instant updatedAt
) {
    public static AdminUserView from(User user) {
        return new AdminUserView(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public String roleLabel() {
        return role == Role.ADMIN ? "Quản trị" : "Học viên";
    }
}
