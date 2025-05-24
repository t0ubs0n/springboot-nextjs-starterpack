package com.toubson.modulith.user.api;

import com.toubson.modulith.user.application.UserService;
import com.toubson.modulith.user.domain.User;
import com.toubson.modulith.user.dto.CreateUserRequest;
import com.toubson.modulith.user.dto.UserResponse;
import com.toubson.modulith.user.mapper.UserToUserResponseMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management API")
public class UserController {

    private final UserService service;

    @Operation(summary = "Test endpoint", description = "Simple test endpoint to verify the API is working")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Test successful")
    })
    @GetMapping("/test")
    public String test() {
        return "test";
    }

    @Operation(summary = "Create user", description = "Creates a new user with the provided information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        User user = service.createUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                new HashSet<>(request.getRoles())
        );

        return new ResponseEntity<>(UserToUserResponseMapper.mapToResponse(user), HttpStatus.CREATED);
    }

}
