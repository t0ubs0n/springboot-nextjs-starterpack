package com.toubson.modulith.user.application;

import com.toubson.modulith.user.domain.User;
import com.toubson.modulith.user.domain.UserRole;
import com.toubson.modulith.user.dto.RegistrationRequest;
import com.toubson.modulith.user.dto.UpdateUserDetailsRequest;
import com.toubson.modulith.user.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private final String testUsername = "testuser";
    private final String testEmail = "test@example.com";
    private final String testPassword = "password";
    private final String encodedPassword = "encodedPassword";
    private final String testToken = "testToken";
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationEventPublisher publisher;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;
    @InjectMocks
    private UserService userService;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Set up test user
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername(testUsername);
        testUser.setEmail(testEmail);
        testUser.setPassword(encodedPassword);
        testUser.setEnabled(false);
        testUser.setEmailVerified(false);

        Set<UserRole> roles = new HashSet<>();
        roles.add(UserRole.ROLE_USER);
        testUser.setRoles(roles);

        // Set verification token properties
        ReflectionTestUtils.setField(userService, "verificationTokenExpiration", 86400000L);
        ReflectionTestUtils.setField(userService, "resetPasswordTokenExpiration", 3600000L);

        // Mock password encoder
        lenient().when(passwordEncoder.encode(anyString())).thenReturn(encodedPassword);
    }

    @Test
    void createUser_Success() {
        // Arrange
        when(userRepository.existsByUsername(testUsername)).thenReturn(false);
        when(userRepository.existsByEmail(testEmail)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        Set<UserRole> roles = new HashSet<>();
        roles.add(UserRole.ROLE_USER);

        // Act
        User result = userService.createUser(testUsername, testEmail, testPassword, roles);

        // Assert
        assertNotNull(result);
        assertEquals(testUsername, result.getUsername());
        assertEquals(testEmail, result.getEmail());
        assertEquals(encodedPassword, result.getPassword());
        assertTrue(result.getRoles().contains(UserRole.ROLE_USER));

        verify(userRepository).existsByUsername(testUsername);
        verify(userRepository).existsByEmail(testEmail);
        verify(userRepository).save(any(User.class));
        verify(publisher).publishEvent(any(UserCreatedEvent.class));
    }

    @Test
    void createUser_UsernameExists_ThrowsException() {
        // Arrange
        when(userRepository.existsByUsername(testUsername)).thenReturn(true);

        Set<UserRole> roles = new HashSet<>();
        roles.add(UserRole.ROLE_USER);

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                userService.createUser(testUsername, testEmail, testPassword, roles)
        );

        assertEquals("Username already exists: " + testUsername, exception.getMessage());

        verify(userRepository).existsByUsername(testUsername);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_EmailExists_ThrowsException() {
        // Arrange
        when(userRepository.existsByUsername(testUsername)).thenReturn(false);
        when(userRepository.existsByEmail(testEmail)).thenReturn(true);

        Set<UserRole> roles = new HashSet<>();
        roles.add(UserRole.ROLE_USER);

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                userService.createUser(testUsername, testEmail, testPassword, roles)
        );

        assertEquals("Email already exists: " + testEmail, exception.getMessage());

        verify(userRepository).existsByUsername(testUsername);
        verify(userRepository).existsByEmail(testEmail);
        verify(userRepository, never()).save(any(User.class));
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

        when(userRepository.existsByUsername(testUsername)).thenReturn(false);
        when(userRepository.existsByEmail(testEmail)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.registerUser(request);

        // Assert
        assertNotNull(result);
        assertEquals(testUsername, result.getUsername());
        assertEquals(testEmail, result.getEmail());

        verify(userRepository).existsByUsername(testUsername);
        verify(userRepository).existsByEmail(testEmail);
        verify(userRepository).save(any(User.class));
        verify(emailService).sendVerificationEmail(eq(testEmail), anyString());
        verify(publisher).publishEvent(any(UserCreatedEvent.class));
    }

    @Test
    void verifyEmail_Success() {
        // Arrange
        testUser.setVerificationToken(testToken);
        testUser.setVerificationTokenExpiryDate(Instant.now().plusMillis(10000));

        when(userRepository.findByVerificationToken(testToken)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.verifyEmail(testToken);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmailVerified());
        assertTrue(result.isEnabled());
        assertNull(result.getVerificationToken());
        assertNull(result.getVerificationTokenExpiryDate());

        verify(userRepository).findByVerificationToken(testToken);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void verifyEmail_TokenExpired_ThrowsException() {
        // Arrange
        testUser.setVerificationToken(testToken);
        testUser.setVerificationTokenExpiryDate(Instant.now().minusMillis(10000));

        when(userRepository.findByVerificationToken(testToken)).thenReturn(Optional.of(testUser));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                userService.verifyEmail(testToken)
        );

        assertEquals("Verification token has expired", exception.getMessage());

        verify(userRepository).findByVerificationToken(testToken);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void initiatePasswordReset_Success() {
        // Arrange
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        userService.initiatePasswordReset(testEmail);

        // Assert
        verify(userRepository).findByEmail(testEmail);
        verify(userRepository).save(any(User.class));
        verify(emailService).sendPasswordResetEmail(eq(testEmail), anyString());
    }

    @Test
    void resetPassword_Success() {
        // Arrange
        testUser.setResetPasswordToken(testToken);
        testUser.setResetPasswordTokenExpiryDate(Instant.now().plusMillis(10000));

        when(userRepository.findByResetPasswordToken(testToken)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.resetPassword(testToken, "newPassword", "newPassword");

        // Assert
        assertNotNull(result);
        assertEquals(encodedPassword, result.getPassword());
        assertNull(result.getResetPasswordToken());
        assertNull(result.getResetPasswordTokenExpiryDate());

        verify(userRepository).findByResetPasswordToken(testToken);
        verify(passwordEncoder).encode("newPassword");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updatePassword_Success() {
        // Arrange
        when(passwordEncoder.matches(testPassword, encodedPassword)).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.updatePassword(testUser, testPassword, "newPassword", "newPassword");

        // Assert
        assertNotNull(result);
        assertEquals(encodedPassword, result.getPassword());

        verify(passwordEncoder).matches(testPassword, encodedPassword);
        verify(passwordEncoder).encode("newPassword");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUserDetails_Success() {
        // Arrange
        UpdateUserDetailsRequest request = new UpdateUserDetailsRequest();
        request.setFirstName("Updated");
        request.setLastName("User");

        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.updateUserDetails(testUser, request);

        // Assert
        assertNotNull(result);
        assertEquals("Updated", result.getFirstName());
        assertEquals("User", result.getLastName());

        verify(userRepository).save(any(User.class));
    }
}
