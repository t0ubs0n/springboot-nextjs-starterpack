package com.toubson.modulith.identity.application;

import com.toubson.modulith.identity.domain.User;
import com.toubson.modulith.identity.domain.UserRole;
import com.toubson.modulith.identity.dto.RegistrationRequest;
import com.toubson.modulith.identity.dto.UpdateUserDetailsRequest;
import com.toubson.modulith.identity.infrastructure.UserRepository;
import com.toubson.modulith.shared.events.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher publisher;
    private final PasswordEncoder passwordEncoder;
//    private final EmailService emailService;

    @Value("${app.token.verification.expiration:86400000}")
    private long verificationTokenExpiration;

    @Value("${app.token.reset-password.expiration:3600000}")
    private long resetPasswordTokenExpiration;

    @Transactional
    public User createUser(String username, String email, String password, Set<UserRole> roles) {
        log.debug("Creating new user with username: {} and email: {}", username, email);

        // Check if username or email already exists·
        if (userRepository.existsByUsername(username)) {
            log.debug("Username already exists: {}", username);
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            log.debug("Email already exists: {}", email);
            throw new IllegalArgumentException("Email already exists: " + email);
        }

        var user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));

        // Set roles or default to USER if none provided
        if (roles != null && !roles.isEmpty()) {
            log.debug("Setting custom roles for user: {}", roles);
            user.setRoles(roles);
        } else {
            log.debug("Setting default role ROLE_USER for user");
            Set<UserRole> defaultRoles = new HashSet<>();
            defaultRoles.add(UserRole.ROLE_USER);
            user.setRoles(defaultRoles);
        }

        User savedUser = userRepository.save(user);
        log.debug("User saved to database with ID: {}", savedUser.getId());

        // Publish event
        log.debug("Publishing UserCreatedEvent for username: {}", savedUser.getUsername());
        publisher.publishEvent(new UserCreatedEvent(savedUser.getUsername()));

        return savedUser;
    }

    @Transactional
    public User registerUser(RegistrationRequest request) {
        log.debug("Processing user registration request for username: {} and email: {}", request.getUsername(), request.getEmail());

        // Validate password confirmation
        if (!request.getPassword().equals(request.getPasswordConfirmation())) {
            log.debug("Password confirmation failed for registration request");
            throw new IllegalArgumentException("Password and confirmation do not match");
        }

        // Check if username or email already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            log.debug("Username already exists during registration: {}", request.getUsername());
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            log.debug("Email already exists during registration: {}", request.getEmail());
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        log.debug("Creating new user from registration request");
        // Create user
        var user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        // Set default role
        log.debug("Setting default role ROLE_USER for new registration");
        Set<UserRole> defaultRoles = new HashSet<>();
        defaultRoles.add(UserRole.ROLE_USER);
        user.setRoles(defaultRoles);

        // Generate verification token
        log.debug("Generating verification token for new user");
        String verificationToken = generateToken();
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiryDate(Instant.now().plusMillis(verificationTokenExpiration));

        // Save user
        User savedUser = userRepository.save(user);
        log.debug("User registered and saved to database with ID: {}", savedUser.getId());

        // Send verification email
//        log.debug("Sending verification email to: {}", user.getEmail());
//        emailService.sendVerificationEmail(user.getEmail(), verificationToken);

        // Publish event
        log.debug("Publishing UserCreatedEvent for newly registered user: {}", savedUser.getUsername());
        publisher.publishEvent(new UserCreatedEvent(savedUser.getUsername()));

        return savedUser;
    }

    @Transactional
    public User verifyEmail(String token) {
        log.debug("Processing email verification with token: {}", token);

        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> {
                    log.debug("Invalid verification token: {}", token);
                    return new IllegalArgumentException("Invalid verification token");
                });

        log.debug("Found user with ID: {} for verification token", user.getId());

        // Check if token is expired
        if (user.getVerificationTokenExpiryDate().isBefore(Instant.now())) {
            log.debug("Verification token has expired for user ID: {}", user.getId());
            throw new IllegalArgumentException("Verification token has expired");
        }

        // Update user
        log.debug("Verifying email for user ID: {}", user.getId());
        user.setEmailVerified(true);
        user.setEnabled(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiryDate(null);
        user.setUpdatedAt(Instant.now());

        User savedUser = userRepository.save(user);
        log.debug("Email verification completed successfully for user ID: {}", savedUser.getId());

        return savedUser;
    }

    @Transactional
    public void initiatePasswordReset(String email) {
        log.debug("Initiating password reset process for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.debug("User not found with email: {}", email);
                    return new IllegalArgumentException("User not found with email: " + email);
                });

        log.debug("Found user with ID: {} for password reset", user.getId());

        // Generate reset token
        log.debug("Generating password reset token for user ID: {}", user.getId());
        String resetToken = generateToken();
        user.setResetPasswordToken(resetToken);
        user.setResetPasswordTokenExpiryDate(Instant.now().plusMillis(resetPasswordTokenExpiration));
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);
        log.debug("Password reset token saved for user ID: {}", user.getId());

        // Send reset email
//        log.debug("Sending password reset email to: {}", user.getEmail());
//        emailService.sendPasswordResetEmail(user.getEmail(), resetToken);

        log.debug("Password reset process initiated successfully for user ID: {}", user.getId());
    }

    @Transactional
    public User resetPassword(String token, String newPassword, String confirmPassword) {
        log.debug("Processing password reset with token");

        // Validate password confirmation //TODO Contrôle de surface à faire par le front ?
        if (!newPassword.equals(confirmPassword)) {
            log.debug("Password and confirmation do not match during reset");
            throw new IllegalArgumentException("Password and confirmation do not match");
        }

        User user = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> {
                    log.debug("Invalid reset token: {}", token);
                    return new IllegalArgumentException("Invalid reset token");
                });

        log.debug("Found user with ID: {} for password reset token", user.getId());

        // Check if token is expired
        if (user.getResetPasswordTokenExpiryDate().isBefore(Instant.now())) {
            log.debug("Reset token has expired for user ID: {}", user.getId());
            throw new IllegalArgumentException("Reset token has expired");
        }

        // Update user
        log.debug("Resetting password for user ID: {}", user.getId());
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiryDate(null);
        user.setUpdatedAt(Instant.now());

        User savedUser = userRepository.save(user);
        log.debug("Password reset completed successfully for user ID: {}", savedUser.getId());

        return savedUser;
    }

    @Transactional
    public User updatePassword(User user, String currentPassword, String newPassword, String confirmPassword) {
        log.debug("Processing password update for user ID: {}", user.getId());

        // Validate password confirmation //TODO Contrôle de surface à faire par le front ?
        if (!newPassword.equals(confirmPassword)) {
            log.debug("Password and confirmation do not match during update for user ID: {}", user.getId());
            throw new IllegalArgumentException("Password and confirmation do not match");
        }

        // Validate current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            log.debug("Current password is incorrect for user ID: {}", user.getId());
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Update user
        log.debug("Updating password for user ID: {}", user.getId());
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(Instant.now());

        User savedUser = userRepository.save(user);
        log.debug("Password updated successfully for user ID: {}", savedUser.getId());

        return savedUser;
    }

    @Transactional
    public User updateUserDetails(User user, UpdateUserDetailsRequest request) {
        log.debug("Processing user details update for user ID: {}", user.getId());

        // Update user
        log.debug("Updating first name and last name for user ID: {}", user.getId());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setUpdatedAt(Instant.now());

        User savedUser = userRepository.save(user);
        log.debug("User details updated successfully for user ID: {}", savedUser.getId());

        return savedUser;
    }

    @Transactional
    public String generateNewVerificationToken(String email) {
        log.debug("Generating new verification token for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.debug("User not found with email: {}", email);
                    return new IllegalArgumentException("User not found with email: " + email);
                });

        log.debug("Found user with ID: {} for new verification token", user.getId());

        // Generate new verification token
        log.debug("Creating new verification token for user ID: {}", user.getId());
        String verificationToken = generateToken();
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiryDate(Instant.now().plusMillis(verificationTokenExpiration));
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);
        log.debug("New verification token saved for user ID: {}", user.getId());

        // Send verification email
//        log.debug("Sending verification email to: {}", user.getEmail());
//        emailService.sendVerificationEmail(user.getEmail(), verificationToken);

        log.debug("New verification token generated successfully for user ID: {}", user.getId());
        return verificationToken;
    }

    private String generateToken() {
        return UUID.randomUUID().toString();
    }

}
