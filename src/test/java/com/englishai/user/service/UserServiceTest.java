package com.englishai.user.service;

import com.englishai.common.enums.Level;
import com.englishai.exercise.dto.LearnerProfileDto;
import com.englishai.exercise.enums.AssessmentStatus;
import com.englishai.exercise.service.LearnerLevelService;
import com.englishai.user.dto.AdminUserView;
import com.englishai.user.entity.Role;
import com.englishai.user.entity.User;
import com.englishai.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private LearnerLevelService learnerLevelService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, learnerLevelService);
    }

    @Test
    void enrichesAdminPageWithLearnerProfilesInBatch() {
        User learner = user(Role.USER, "learner@example.com");
        User admin = user(Role.ADMIN, "admin@example.com");
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(learner, admin), pageable, 2));
        when(learnerLevelService.getProfiles(eq(java.util.Set.of(learner.getId()))))
                .thenReturn(Map.of(learner.getId(), profile(Level.INTERMEDIATE)));

        Page<AdminUserView> result = userService.searchUsers(null, null, null, null, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().getFirst().learnerProfile().level()).isEqualTo(Level.INTERMEDIATE);
        assertThat(result.getContent().getLast().learnerProfile()).isNull();
    }

    @Test
    void filtersLearnersByComputedLevelBeforePagination() {
        User beginner = user(Role.USER, "beginner@example.com");
        User intermediate = user(Role.USER, "intermediate@example.com");
        User admin = user(Role.ADMIN, "admin@example.com");
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt"));
        when(userRepository.findAll(any(Specification.class), eq(pageable.getSort())))
                .thenReturn(List.of(beginner, intermediate, admin));
        when(learnerLevelService.getProfiles(any()))
                .thenReturn(Map.of(
                        beginner.getId(), profile(Level.BEGINNER),
                        intermediate.getId(), profile(Level.INTERMEDIATE)));

        Page<AdminUserView> result = userService.searchUsers(
                null,
                null,
                null,
                Level.INTERMEDIATE,
                pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().email()).isEqualTo("intermediate@example.com");
    }

    @Test
    void countsLearnersByComputedLevelForAdminSummary() {
        User beginner = user(Role.USER, "beginner@example.com");
        User advanced = user(Role.USER, "advanced@example.com");
        when(userRepository.findAll(any(Specification.class))).thenReturn(List.of(beginner, advanced));
        when(learnerLevelService.getProfiles(any()))
                .thenReturn(Map.of(
                        beginner.getId(), profile(Level.BEGINNER),
                        advanced.getId(), profile(Level.ADVANCED)));

        Map<Level, Long> counts = userService.countLearnersByLevel();

        assertThat(counts).containsEntry(Level.BEGINNER, 1L);
        assertThat(counts).containsEntry(Level.INTERMEDIATE, 0L);
        assertThat(counts).containsEntry(Level.ADVANCED, 1L);
    }

    @Test
    void loadsDetailedLearnerViewWithComputedProfile() {
        User learner = user(Role.USER, "learner@example.com");
        LearnerProfileDto learnerProfile = profile(Level.ADVANCED);
        when(userRepository.findById(learner.getId())).thenReturn(java.util.Optional.of(learner));
        when(learnerLevelService.getProfile(learner.getId())).thenReturn(learnerProfile);

        AdminUserView result = userService.getAdminUserView(learner.getId());

        assertThat(result.id()).isEqualTo(learner.getId());
        assertThat(result.learnerProfile()).isSameAs(learnerProfile);
    }

    @Test
    void doesNotCalculateLearnerProfileForAdminDetail() {
        User admin = user(Role.ADMIN, "admin@example.com");
        when(userRepository.findById(admin.getId())).thenReturn(java.util.Optional.of(admin));

        AdminUserView result = userService.getAdminUserView(admin.getId());

        assertThat(result.learnerProfile()).isNull();
    }

    private User user(Role role, String email) {
        User user = User.builder()
                .fullName(email)
                .email(email)
                .role(role)
                .enabled(true)
                .build();
        user.setId(UUID.randomUUID());
        return user;
    }

    private LearnerProfileDto profile(Level level) {
        return new LearnerProfileDto(
                level,
                8,
                82,
                0,
                0,
                AssessmentStatus.PROVISIONAL,
                75,
                Map.of(),
                "Test profile");
    }
}
