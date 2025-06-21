package com.toubson.modulith.identity.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toubson.modulith.identity.application.AuthenticationService;
import com.toubson.modulith.identity.application.UserService;
import com.toubson.modulith.identity.domain.User;
import com.toubson.modulith.identity.domain.UserRole;
import com.toubson.modulith.identity.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    private final String testUsername = "testuser";
    private final String testEmail = "test@example.com";
    private final String testPassword = "password";
    private final String testToken = "test-token";
    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private UserService userService;
    @Mock
    private AuthenticationService authenticationService;
    @InjectMocks
    private AccountController accountController;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Set up MockMvc
        mockMvc = MockMvcBuilders.standaloneSetup(accountController).build();

        // Set up test user
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername(testUsername);
        testUser.setEmail(testEmail);
        testUser.setPassword(testPassword);
        testUser.setEnabled(false);
        testUser.setEmailVerified(false);
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setCreatedAt(Instant.now());

        Set<UserRole> roles = new HashSet<>();
        roles.add(UserRole.ROLE_USER);
        testUser.setRoles(roles);

        // Reset mocks
        reset(userService);
        reset(authenticationService);
    }

    @Test
    void register_Success() throws Exception {
        // Arrange
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername(testUsername);
        request.setEmail(testEmail);
        request.setPassword(testPassword);
        request.setPasswordConfirmation(testPassword);
        request.setFirstName("Test");
        request.setLastName("User");

        when(userService.registerUser(any(RegistrationRequest.class))).thenReturn(testUser);

        // Act & Assert
        mockMvc.perform(post("/auth/account/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(testUsername))
                .andExpect(jsonPath("$.email").value(testEmail));
    }

    @Test
    void verifyEmail_Success() throws Exception {
        // Arrange
        EmailVerificationRequest request = new EmailVerificationRequest();
        request.setToken(testToken);

        testUser.setEnabled(true);
        testUser.setEmailVerified(true);

        when(userService.verifyEmail(anyString())).thenReturn(testUser);

        // Act & Assert
        mockMvc.perform(post("/auth/account/verify-email")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(testUsername))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.emailVerified").value(true));
    }

    @Test
    void resendVerification_Success() throws Exception {
        // Arrange
        when(userService.generateNewVerificationToken(anyString())).thenReturn(testToken);

        // Act & Assert
        mockMvc.perform(post("/auth/account/resend-verification")
                        .with(csrf())
                        .param("email", testEmail))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Verification email sent successfully"));
    }

    @Test
    void forgotPassword_Success() throws Exception {
        // Arrange
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(testEmail);

        // Act & Assert
        mockMvc.perform(post("/auth/account/forgot-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset email sent successfully"));
    }

    @Test
    void resetPassword_Success() throws Exception {
        // Arrange
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(testToken);
        request.setNewPassword("newpassword");
        request.setConfirmPassword("newpassword");

        when(userService.resetPassword(anyString(), anyString(), anyString())).thenReturn(testUser);

        // Act & Assert
        mockMvc.perform(post("/auth/account/reset-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(testUsername));
    }

    @Test
    @WithMockUser(username = "testuser")
    void updatePassword_Success() throws Exception {
        // Arrange
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setCurrentPassword(testPassword);
        request.setNewPassword("newpassword");
        request.setConfirmPassword("newpassword");

        when(userService.updatePassword(any(User.class), anyString(), anyString(), anyString())).thenReturn(testUser);

        // Act & Assert
        mockMvc.perform(post("/auth/account/update-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(testUsername));
    }

    @Test
    @WithMockUser(username = "testuser")
    void updateUserDetails_Success() throws Exception {
        // Arrange
        UpdateUserDetailsRequest request = new UpdateUserDetailsRequest();
        request.setFirstName("Updated");
        request.setLastName("User");

        testUser.setFirstName("Updated");
        testUser.setLastName("User");

        when(userService.updateUserDetails(any(User.class), any(UpdateUserDetailsRequest.class))).thenReturn(testUser);

        // Act & Assert
        mockMvc.perform(put("/auth/account/update-details")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(testUsername))
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.lastName").value("User"));
    }
}
