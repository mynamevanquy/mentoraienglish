package com.englishai.auth.controller;

import com.englishai.auth.service.PasswordResetService;
import com.englishai.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private UserService userService;
    @Mock private PasswordResetService passwordResetService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(userService, passwordResetService))
                .build();
    }

    @Test
    void submitsForgotPasswordWithGenericSuccessMessage() throws Exception {
        mockMvc.perform(post("/forgot-password").param("email", "learner@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forgot-password"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(passwordResetService).requestReset("learner@example.com");
    }

    @Test
    void rejectsInvalidForgotPasswordEmail() throws Exception {
        mockMvc.perform(post("/forgot-password").param("email", "not-an-email"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/forgot-password"))
                .andExpect(model().attributeHasFieldErrors("forgotPasswordRequest", "email"));
    }

    @Test
    void displaysResetFormForValidToken() throws Exception {
        when(passwordResetService.isTokenValid("valid-token")).thenReturn(true);

        mockMvc.perform(get("/reset-password").param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/reset-password"))
                .andExpect(model().attribute("tokenValid", true));
    }

    @Test
    void resetsMatchingPasswordAndRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/reset-password")
                        .param("token", "valid-token")
                        .param("password", "new-password")
                        .param("confirmPassword", "new-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(passwordResetService).resetPassword("valid-token", "new-password");
    }

    @Test
    void keepsFormWhenPasswordConfirmationDoesNotMatch() throws Exception {
        when(passwordResetService.isTokenValid("valid-token")).thenReturn(true);

        mockMvc.perform(post("/reset-password")
                        .param("token", "valid-token")
                        .param("password", "new-password")
                        .param("confirmPassword", "different-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/reset-password"))
                .andExpect(model().attributeHasFieldErrors("resetPasswordRequest", "confirmPassword"))
                .andExpect(model().attribute("tokenValid", true));
    }
}
