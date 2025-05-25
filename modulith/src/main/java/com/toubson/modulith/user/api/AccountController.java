package com.toubson.modulith.user.api;

import com.toubson.modulith.security.utils.AuthenticationHelper;
import com.toubson.modulith.user.application.UserService;
import com.toubson.modulith.user.domain.User;
import com.toubson.modulith.user.dto.*;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth/account")
@RequiredArgsConstructor
@Tag(name = "Account", description = "Account management API")
public class AccountController {

    private final UserService userService;
    private final AuthenticationHelper authenticationHelper;

    @Operation(summary = "Register a new user", description = "Creates a new user account and sends a verification email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegistrationRequest request) {
        User user = userService.registerUser(request);
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
        User user = userService.verifyEmail(request.getToken());
        return ResponseEntity.ok(UserToUserResponseMapper.mapToResponse(user));
    }

    @Operation(summary = "Resend verification email", description = "Generates a new verification token and sends a new verification email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verification email sent successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(@RequestParam String email) {
        userService.generateNewVerificationToken(email);
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
        userService.initiatePasswordReset(request.getEmail());
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
        User user = userService.resetPassword(request.getToken(), request.getNewPassword(), request.getConfirmPassword());
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
        User updatedUser = userService.updatePassword(
                user,
                request.getCurrentPassword(),
                request.getNewPassword(),
                request.getConfirmPassword()
        );
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
        User updatedUser = userService.updateUserDetails(user, request);
        return ResponseEntity.ok(UserToUserResponseMapper.mapToResponse(updatedUser));
    }
}