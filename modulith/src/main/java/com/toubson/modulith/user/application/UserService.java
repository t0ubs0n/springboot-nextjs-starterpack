package com.toubson.modulith.user.application;

import com.toubson.modulith.user.domain.User;
import com.toubson.modulith.user.domain.UserRole;
import com.toubson.modulith.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher publisher;
    private final PasswordEncoder passwordEncoder;


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

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    private String generateRandomPassword() {
        return java.util.UUID.randomUUID().toString().substring(0, 8);
    }
}
