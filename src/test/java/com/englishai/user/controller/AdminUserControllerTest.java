package com.englishai.user.controller;

import com.englishai.user.dto.AdminUserView;
import com.englishai.user.entity.Role;
import com.englishai.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock private UserService userService;
    @Mock private SessionRegistry sessionRegistry;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminUserController(userService, sessionRegistry))
                .build();
    }

    @Test
    void createFormFieldsDoNotBecomeListFiltersWhenValidationFails() throws Exception {
        when(userService.searchUsers(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(userService.countLearnersByLevel()).thenReturn(Map.of());

        mockMvc.perform(post("/admin/users")
                        .param("fullName", "")
                        .param("email", "")
                        .param("password", "")
                        .param("role", "USER")
                        .param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/index"))
                .andExpect(model().attribute("selectedRole", nullValue()))
                .andExpect(model().attribute("selectedEnabled", nullValue()));

        verify(userService).searchUsers(isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void opensDedicatedUserDetailWithStandardForms() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.getAdminUserView(userId)).thenReturn(new AdminUserView(
                userId,
                "Nguyễn Văn A",
                "learner@example.com",
                Role.USER,
                true,
                null,
                null,
                null
        ));

        mockMvc.perform(get("/admin/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/detail"))
                .andExpect(model().attribute("user", instanceOf(AdminUserView.class)))
                .andExpect(model().attributeExists("updateRequest", "resetPasswordRequest", "roles"));
    }
}
