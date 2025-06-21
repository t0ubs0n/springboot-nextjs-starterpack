package com.toubson.modulith.identity.api;

import com.toubson.modulith.identity.application.UserService;
import com.toubson.modulith.identity.domain.User;
import com.toubson.modulith.identity.dto.*;
import com.toubson.modulith.identity.mapper.UserToUserResponseMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth/account")
@RequiredArgsConstructor
@Tag(name = "Account", description = "Account management API")
public class AccountController {

    private final UserService userService;

    @Operation(summary = "Register a new user", description = "Creates a new user account and sends a verification email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegistrationRequest request) {
        log.debug("Starting user registration process for email: {}", request.getEmail());
        User user = userService.registerUser(request);
        log.debug("User registration completed successfully for user ID: {}", user.getId());
        return new ResponseEntity<>(UserToUserResponseMapper.mapToResponse(user), HttpStatus.CREATED);
    }

    @Operation(summary = "Verify email", description = "Verifies a user's email address using the token sent to their email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email verified successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    @PostMapping("/verify-email")
    public ResponseEntity<UserResponse> verifyEmail(@Valid @RequestBody EmailVerificationRequest request) {
        log.debug("Processing email verification with token");
        User user = userService.verifyEmail(request.getToken());
        log.debug("Email verification successful for user ID: {}", user.getId());
        return ResponseEntity.ok(UserToUserResponseMapper.mapToResponse(user));
    }

    @Operation(summary = "Resend verification email", description = "Generates a new verification token and sends a new verification email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verification email sent successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(@RequestParam String email) {
        log.debug("Resending verification email to: {}", email);
        userService.generateNewVerificationToken(email);
        log.debug("Verification email resent successfully to: {}", email);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Verification email sent successfully");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Forgot password", description = "Initiates the password reset process by sending a reset email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset email sent successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.debug("Initiating password reset process for email: {}", request.getEmail());
        userService.initiatePasswordReset(request.getEmail());
        log.debug("Password reset email sent successfully to: {}", request.getEmail());
        Map<String, String> response = new HashMap<>();
        response.put("message", "Password reset email sent successfully");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reset password", description = "Resets a user's password using the token sent to their email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token, or passwords don't match")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<UserResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.debug("Processing password reset request with token");
        User user = userService.resetPassword(request.getToken(), request.getNewPassword(), request.getConfirmPassword());
        log.debug("Password reset successful for user ID: {}", user.getId());
        return ResponseEntity.ok(UserToUserResponseMapper.mapToResponse(user));
    }

    @Operation(summary = "Update password", description = "Updates the password of the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid current password or passwords don't match")
    })
    @PostMapping("/update-password")
    public ResponseEntity<UserResponse> updatePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdatePasswordRequest request) {
        log.debug("Processing password update request for user ID: {}", user.getId());
        User updatedUser = userService.updatePassword(
                user,
                request.getCurrentPassword(),
                request.getNewPassword(),
                request.getConfirmPassword()
        );
        log.debug("Password updated successfully for user ID: {}", user.getId());
        return ResponseEntity.ok(UserToUserResponseMapper.mapToResponse(updatedUser));
    }

    @Operation(summary = "Update user details", description = "Updates the basic details of the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User details updated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PutMapping("/update-details")
    public ResponseEntity<UserResponse> updateUserDetails(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateUserDetailsRequest request) {
        log.debug("Processing user details update for user ID: {}", user.getId());
        User updatedUser = userService.updateUserDetails(user, request);
        log.debug("User details updated successfully for user ID: {}", user.getId());
        return ResponseEntity.ok(UserToUserResponseMapper.mapToResponse(updatedUser));
    }
}
