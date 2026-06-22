package com.englishai.user.service;

import com.englishai.auth.dto.RegisterRequest;
import com.englishai.common.enums.Level;
import com.englishai.exercise.dto.LearnerProfileDto;
import com.englishai.exercise.service.LearnerLevelService;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Business logic for user account management.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LearnerLevelService learnerLevelService;

    @Transactional(readOnly = true)
    public Page<AdminUserView> searchUsers(
            String keyword,
            Role role,
            Boolean enabled,
            Level learnerLevel,
            Pageable pageable) {
        Specification<User> spec = buildAdminSpecification(keyword, role, enabled);

        if (learnerLevel == null) {
            Page<User> users = userRepository.findAll(spec, pageable);
            Map<UUID, LearnerProfileDto> profiles = loadLearnerProfiles(users.getContent());
            return users.map(user -> AdminUserView.from(user, profiles.get(user.getId())));
        }

        List<User> candidates = userRepository.findAll(spec, pageable.getSort());
        Map<UUID, LearnerProfileDto> profiles = loadLearnerProfiles(candidates);
        List<AdminUserView> filtered = candidates.stream()
                .filter(user -> user.getRole() == Role.USER)
                .filter(user -> profiles.get(user.getId()).level() == learnerLevel)
                .map(user -> AdminUserView.from(user, profiles.get(user.getId())))
                .toList();

        int start = Math.min((int) pageable.getOffset(), filtered.size());
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
    }

    @Transactional(readOnly = true)
    public Map<Level, Long> countLearnersByLevel() {
        List<User> learners = userRepository.findAll(
                (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("role"), Role.USER));
        Map<UUID, LearnerProfileDto> profiles = loadLearnerProfiles(learners);
        Map<Level, Long> counts = new EnumMap<>(Level.class);
        for (Level level : Level.values()) {
            counts.put(level, 0L);
        }
        profiles.values().forEach(profile -> counts.compute(
                profile.level(),
                (level, count) -> count == null ? 1L : count + 1));
        return counts;
    }

    private Specification<User> buildAdminSpecification(String keyword, Role role, Boolean enabled) {
        Specification<User> spec = Specification.where(null);
        if (StringUtils.hasText(keyword)) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern)));
        }
        if (role != null) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("role"), role));
        }
        if (enabled != null) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("enabled"), enabled));
        }
        return spec;
    }

    private Map<UUID, LearnerProfileDto> loadLearnerProfiles(List<User> users) {
        Set<UUID> learnerIds = users.stream()
                .filter(user -> user.getRole() == Role.USER)
                .map(User::getId)
                .collect(Collectors.toSet());
        return learnerLevelService.getProfiles(learnerIds);
    }

    @Transactional(readOnly = true)
    public AdminUserView getAdminUserView(UUID userId) {
        User user = getUserForAdmin(userId);
        LearnerProfileDto learnerProfile = user.getRole() == Role.USER
                ? learnerLevelService.getProfile(userId)
                : null;
        return AdminUserView.from(user, learnerProfile);
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
