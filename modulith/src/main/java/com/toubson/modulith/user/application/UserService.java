package com.toubson.modulith.user.application;

import com.toubson.modulith.user.domain.User;
import com.toubson.modulith.user.domain.UserRole;
import com.toubson.modulith.user.dto.RegistrationRequest;
import com.toubson.modulith.user.dto.UpdateUserDetailsRequest;
import com.toubson.modulith.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher publisher;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.token.verification.expiration:86400000}")
    private long verificationTokenExpiration;

    @Value("${app.token.reset-password.expiration:3600000}")
    private long resetPasswordTokenExpiration;

    @Transactional
    public User createUser(String username, String email, String password, Set<UserRole> roles) {
        // Check if username or email already exists
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }

        var user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));

        // Set roles or default to USER if none provided
        if (roles != null && !roles.isEmpty()) {
            user.setRoles(roles);
        } else {
            Set<UserRole> defaultRoles = new HashSet<>();
            defaultRoles.add(UserRole.ROLE_USER);
            user.setRoles(defaultRoles);
        }

        User savedUser = userRepository.save(user);

        // Publish event
        publisher.publishEvent(new UserCreatedEvent(savedUser.getUsername()));

        return savedUser;
    }

    @Transactional
    public User registerUser(RegistrationRequest request) {
        // Validate password confirmation
        if (!request.getPassword().equals(request.getPasswordConfirmation())) {
            throw new IllegalArgumentException("Password and confirmation do not match");
        }

        // Check if username or email already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        // Create user
        var user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        // Set default role
        Set<UserRole> defaultRoles = new HashSet<>();
        defaultRoles.add(UserRole.ROLE_USER);
        user.setRoles(defaultRoles);

        // Generate verification token
        String verificationToken = generateToken();
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiryDate(Instant.now().plusMillis(verificationTokenExpiration));

        // Save user
        User savedUser = userRepository.save(user);

        // Send verification email
        emailService.sendVerificationEmail(user.getEmail(), verificationToken);

        // Publish event
        publisher.publishEvent(new UserCreatedEvent(savedUser.getUsername()));

        return savedUser;
    }

    @Transactional
    public User verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        // Check if token is expired
        if (user.getVerificationTokenExpiryDate().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Verification token has expired");
        }

        // Update user
        user.setEmailVerified(true);
        user.setEnabled(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiryDate(null);
        user.setUpdatedAt(Instant.now());

        return userRepository.save(user);
    }

    @Transactional
    public void initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        // Generate reset token
        String resetToken = generateToken();
        user.setResetPasswordToken(resetToken);
        user.setResetPasswordTokenExpiryDate(Instant.now().plusMillis(resetPasswordTokenExpiration));
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);

        // Send reset email
        emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
    }

    @Transactional
    public User resetPassword(String token, String newPassword, String confirmPassword) {
        // Validate password confirmation //TODO Contrôle de surface à faire par le front ?
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Password and confirmation do not match");
        }

        User user = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid reset token"));

        // Check if token is expired
        if (user.getResetPasswordTokenExpiryDate().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Reset token has expired");
        }

        // Update user
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiryDate(null);
        user.setUpdatedAt(Instant.now());

        return userRepository.save(user);
    }

    @Transactional
    public User updatePassword(User user, String currentPassword, String newPassword, String confirmPassword) {
        // Validate password confirmation //TODO Contrôle de surface à faire par le front ?
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Password and confirmation do not match");
        }

        // Validate current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Update user
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(Instant.now());

        return userRepository.save(user);
    }

    @Transactional
    public User updateUserDetails(User user, UpdateUserDetailsRequest request) {
        // Update user
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setUpdatedAt(Instant.now());

        return userRepository.save(user);
    }

    @Transactional
    public String generateNewVerificationToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        // Generate new verification token
        String verificationToken = generateToken();
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiryDate(Instant.now().plusMillis(verificationTokenExpiration));
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);

        // Send verification email
        emailService.sendVerificationEmail(user.getEmail(), verificationToken);

        return verificationToken;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    private String generateToken() {
        return UUID.randomUUID().toString();
    }

    private String generateRandomPassword() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
