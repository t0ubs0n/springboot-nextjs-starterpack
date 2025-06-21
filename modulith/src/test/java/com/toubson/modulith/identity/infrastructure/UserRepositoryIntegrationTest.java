package com.toubson.modulith.identity.infrastructure;

import com.toubson.modulith.identity.domain.User;
import com.toubson.modulith.identity.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserRepositoryIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");
    private final String testUsername = "testuser";
    private final String testEmail = "test@example.com";
    private final String testVerificationToken = "verification-token";
    @Autowired
    private UserRepository userRepository;
    private User testUser;

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

        // Create a test user
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername(testUsername);
        testUser.setEmail(testEmail);
        testUser.setPassword("password");
        testUser.setEnabled(false);
        testUser.setEmailVerified(false);
        testUser.setFirstName("Test");
        testUser.setLastName("User");

        // Set verification token
        testUser.setVerificationToken(testVerificationToken);
        testUser.setVerificationTokenExpiryDate(Instant.now().plusSeconds(3600));

        // Set roles
        Set<UserRole> roles = new HashSet<>();
        roles.add(UserRole.ROLE_USER);
        testUser.setRoles(roles);

        // Save the user
        testUser = userRepository.save(testUser);
    }

    @Test
    void findByUsername_ExistingUser_ReturnsUser() {
        // Act
        Optional<User> result = userRepository.findByUsername(testUsername);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testUsername, result.get().getUsername());
    }

    @Test
    void findByUsername_NonExistingUser_ReturnsEmpty() {
        // Act
        Optional<User> result = userRepository.findByUsername("nonexistent");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByEmail_ExistingUser_ReturnsUser() {
        // Act
        Optional<User> result = userRepository.findByEmail(testEmail);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testEmail, result.get().getEmail());
    }

    @Test
    void findByEmail_NonExistingUser_ReturnsEmpty() {
        // Act
        Optional<User> result = userRepository.findByEmail("nonexistent@example.com");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void existsByUsername_ExistingUser_ReturnsTrue() {
        // Act
        boolean result = userRepository.existsByUsername(testUsername);

        // Assert
        assertTrue(result);
    }

    @Test
    void existsByUsername_NonExistingUser_ReturnsFalse() {
        // Act
        boolean result = userRepository.existsByUsername("nonexistent");

        // Assert
        assertFalse(result);
    }

    @Test
    void existsByEmail_ExistingUser_ReturnsTrue() {
        // Act
        boolean result = userRepository.existsByEmail(testEmail);

        // Assert
        assertTrue(result);
    }

    @Test
    void existsByEmail_NonExistingUser_ReturnsFalse() {
        // Act
        boolean result = userRepository.existsByEmail("nonexistent@example.com");

        // Assert
        assertFalse(result);
    }

    @Test
    void findByVerificationToken_ExistingToken_ReturnsUser() {
        // Act
        Optional<User> result = userRepository.findByVerificationToken(testVerificationToken);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testVerificationToken, result.get().getVerificationToken());
    }

    @Test
    void findByVerificationToken_NonExistingToken_ReturnsEmpty() {
        // Act
        Optional<User> result = userRepository.findByVerificationToken("nonexistent-token");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByResetPasswordToken_ExistingToken_ReturnsUser() {
        // Arrange
        String testResetToken = "reset-token";
        testUser.setResetPasswordToken(testResetToken);
        testUser.setResetPasswordTokenExpiryDate(Instant.now().plusSeconds(3600));
        userRepository.save(testUser);

        // Act
        Optional<User> result = userRepository.findByResetPasswordToken(testResetToken);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testResetToken, result.get().getResetPasswordToken());
    }

    @Test
    void findByResetPasswordToken_NonExistingToken_ReturnsEmpty() {
        // Act
        Optional<User> result = userRepository.findByResetPasswordToken("nonexistent-token");

        // Assert
        assertTrue(result.isEmpty());
    }
}
