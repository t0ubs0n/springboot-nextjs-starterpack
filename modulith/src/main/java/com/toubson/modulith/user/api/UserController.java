package com.toubson.modulith.user.api;

import com.toubson.modulith.user.application.UserService;
import com.toubson.modulith.user.domain.User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping
    public User create(@RequestParam String username, @RequestParam String email) {
        return service.createUser(username, email);
    }
}
