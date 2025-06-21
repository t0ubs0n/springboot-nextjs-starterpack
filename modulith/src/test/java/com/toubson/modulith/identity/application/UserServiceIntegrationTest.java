package com.toubson.modulith.identity.application;

import com.toubson.modulith.identity.domain.User;
import com.toubson.modulith.identity.domain.UserRole;
import com.toubson.modulith.identity.dto.RegistrationRequest;
import com.toubson.modulith.identity.dto.UpdateUserDetailsRequest;
import com.toubson.modulith.identity.infrastructure.UserRepository;
import com.toubson.modulith.shared.events.UserCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class UserServiceIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");
    private final String testUsername = "testuser";
    private final String testEmail = "test@example.com";
    private final String testPassword = "password";
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmailService emailService;
    @Autowired
    private ApplicationEventPublisher publisher;

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void setUp() {
        // Clear the repository
        userRepository.deleteAll();

        // Reset mocks
        reset(emailService);
        reset(publisher);

        // Mock email service with lenient stubbing
        lenient().doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());
        lenient().doNothing().when(emailService).sendPasswordResetEmail(anyString(), anyString());

        // Mock event publisher with lenient stubbing
        lenient().doNothing().when(publisher).publishEvent(any(UserCreatedEvent.class));
    }

    @Test
    void createUser_Success() {
        // Arrange
        Set<UserRole> roles = new HashSet<>();
        roles.add(UserRole.ROLE_USER);

        // Act
        User result = userService.createUser(testUsername, testEmail, testPassword, roles);

        // Assert
        assertNotNull(result);
        assertEquals(testUsername, result.getUsername());
        assertEquals(testEmail, result.getEmail());
        assertTrue(result.getRoles().contains(UserRole.ROLE_USER));

        // Verify user was saved to the database
        Optional<User> savedUser = userRepository.findByUsername(testUsername);
        assertTrue(savedUser.isPresent());
        assertEquals(testEmail, savedUser.get().getEmail());
    }

    @Test
    void registerUser_Success() {
        // Arrange
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername(testUsername);
        request.setEmail(testEmail);
        request.setPassword(testPassword);
        request.setPasswordConfirmation(testPassword);
        request.setFirstName("Test");
        request.setLastName("User");

        // Act
        User result = userService.registerUser(request);

        // Assert
        assertNotNull(result);
        assertEquals(testUsername, result.getUsername());
        assertEquals(testEmail, result.getEmail());
        assertEquals("Test", result.getFirstName());
        assertEquals("User", result.getLastName());
        assertFalse(result.isEnabled());
        assertFalse(result.isEmailVerified());
        assertNotNull(result.getVerificationToken());
        assertNotNull(result.getVerificationTokenExpiryDate());

        // Verify user was saved to the database
        Optional<User> savedUser = userRepository.findByUsername(testUsername);
        assertTrue(savedUser.isPresent());
        assertEquals(testEmail, savedUser.get().getEmail());

        // Verify email was sent
        //TODO - Reactiver
//        verify(emailService).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void verifyEmail_Success() {
        // Arrange
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername(testUsername);
        request.setEmail(testEmail);
        request.setPassword(testPassword);
        request.setPasswordConfirmation(testPassword);

        User user = userService.registerUser(request);
        String token = user.getVerificationToken();

        // Act
        User result = userService.verifyEmail(token);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmailVerified());
        assertTrue(result.isEnabled());
        assertNull(result.getVerificationToken());
        assertNull(result.getVerificationTokenExpiryDate());

        // Verify user was updated in the database
        Optional<User> updatedUser = userRepository.findByUsername(testUsername);
        assertTrue(updatedUser.isPresent());
        assertTrue(updatedUser.get().isEmailVerified());
        assertTrue(updatedUser.get().isEnabled());
    }

    @Test
    void initiatePasswordReset_Success() {
        // Arrange
        userService.createUser(testUsername, testEmail, testPassword, Set.of(UserRole.ROLE_USER));

        // Act
        userService.initiatePasswordReset(testEmail);

        // Assert
        Optional<User> updatedUser = userRepository.findByUsername(testUsername);
        assertTrue(updatedUser.isPresent());
        assertNotNull(updatedUser.get().getResetPasswordToken());
        assertNotNull(updatedUser.get().getResetPasswordTokenExpiryDate());

        // Verify email was sent
        //TODO - Reactiver
//        verify(emailService).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void resetPassword_Success() {
        // Arrange
        userService.createUser(testUsername, testEmail, testPassword, Set.of(UserRole.ROLE_USER));
        userService.initiatePasswordReset(testEmail);

        Optional<User> userWithToken = userRepository.findByUsername(testUsername);
        String resetToken = userWithToken.get().getResetPasswordToken();

        // Act
        User result = userService.resetPassword(resetToken, "newPassword", "newPassword");

        // Assert
        assertNotNull(result);
        assertNull(result.getResetPasswordToken());
        assertNull(result.getResetPasswordTokenExpiryDate());

        // Verify user was updated in the database
        Optional<User> updatedUser = userRepository.findByUsername(testUsername);
        assertTrue(updatedUser.isPresent());
        assertNull(updatedUser.get().getResetPasswordToken());
    }

    @Test
    void updateUserDetails_Success() {
        // Arrange
        User user = userService.createUser(testUsername, testEmail, testPassword, Set.of(UserRole.ROLE_USER));

        UpdateUserDetailsRequest request = new UpdateUserDetailsRequest();
        request.setFirstName("Updated");
        request.setLastName("User");

        // Act
        User result = userService.updateUserDetails(user, request);

        // Assert
        assertNotNull(result);
        assertEquals("Updated", result.getFirstName());
        assertEquals("User", result.getLastName());

        // Verify user was updated in the database
        Optional<User> updatedUser = userRepository.findByUsername(testUsername);
        assertTrue(updatedUser.isPresent());
        assertEquals("Updated", updatedUser.get().getFirstName());
        assertEquals("User", updatedUser.get().getLastName());
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public EmailService emailService() {
            return mock(EmailService.class);
        }

        @Bean
        @Primary
        public ApplicationEventPublisher applicationEventPublisher() {
            return mock(ApplicationEventPublisher.class);
        }
    }
}
