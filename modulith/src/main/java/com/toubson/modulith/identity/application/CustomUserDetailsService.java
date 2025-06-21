package com.toubson.modulith.identity.application;

import com.toubson.modulith.identity.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user details for username: {}", username);

        return userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .map(user -> {
                    log.debug("User found with ID: {}", user.getId());
                    return user;
                })
                .orElseThrow(() -> {
                    log.warn("User not found for username: {}", username);
                    return new UsernameNotFoundException("User not found for username: " + username);
                });
    }
}