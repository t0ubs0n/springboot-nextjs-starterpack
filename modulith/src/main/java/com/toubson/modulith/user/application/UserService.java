package com.toubson.modulith.user.application;

import com.toubson.modulith.user.domain.User;
import com.toubson.modulith.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final ApplicationEventPublisher publisher;


    public User createUser(String username, String email) {
        var user = new User();
        user.setUsername(username);
        user.setEmail(email);
        return userRepository.save(user);
    }
}
