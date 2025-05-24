package com.toubson.modulith.security.api;

import com.toubson.modulith.security.config.JwtTokenProvider;
import com.toubson.modulith.security.dto.LoginRequest;
import com.toubson.modulith.security.dto.RefreshTokenRequest;
import com.toubson.modulith.security.dto.TokenResponse;
import com.toubson.modulith.security.exception.AuthenticationException;
import com.toubson.modulith.security.utils.AuthenticationHelper;
import com.toubson.modulith.security.utils.ClientTypeResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication API for managing user sessions")
public class AuthController {

    private final JwtTokenProvider tokenProvider;
    private final ClientTypeResolver clientTypeResolver;
    private final AuthenticationHelper authenticationHelper;

    /**
     * Login endpoint for web clients
     * Returns tokens in HttpOnly cookies
     */
    @Operation(summary = "Web client login", description = "Authenticates a web client and returns tokens in HttpOnly cookies")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully authenticated",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication failed")
    })
    @PostMapping("/web/login")
    public ResponseEntity<TokenResponse> webLogin(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletResponse response) {

        Authentication authentication = authenticationHelper.authenticate(loginRequest.getUsername(), loginRequest.getPassword());
        JwtTokenProvider.TokenPair tokenPair = tokenProvider.createTokenPair(
                authentication, JwtTokenProvider.ClientType.WEB);

        // Add tokens as HttpOnly cookies
        tokenProvider.addAccessTokenCookie(response, tokenPair.getAccessToken());
        tokenProvider.addRefreshTokenCookie(response, tokenPair.getRefreshToken());

        // Return response without tokens in body (they're in cookies)
        return ResponseEntity.ok(TokenResponse.builder()
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getAccessTokenExpirationWeb() / 1000)
                .username(loginRequest.getUsername())
                .message("Authentication successful")
                .build());
    }

    /**
     * Login endpoint for mobile clients
     * Returns tokens in JSON response body
     */
    @Operation(summary = "Mobile client login", description = "Authenticates a mobile client and returns tokens in the response body")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully authenticated",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication failed")
    })
    @PostMapping("/mobile/login")
    public ResponseEntity<TokenResponse> mobileLogin(
            @Valid @RequestBody LoginRequest loginRequest) {

        Authentication authentication = authenticationHelper.authenticate(loginRequest.getUsername(), loginRequest.getPassword());
        JwtTokenProvider.TokenPair tokenPair = tokenProvider.createTokenPair(
                authentication, JwtTokenProvider.ClientType.MOBILE);

        // Return tokens in response body
        return ResponseEntity.ok(TokenResponse.builder()
                .accessToken(tokenPair.getAccessToken())
                .refreshToken(tokenPair.getRefreshToken())
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getAccessTokenExpirationMobile() / 1000)
                .username(loginRequest.getUsername())
                .build());
    }

    /**
     * Refresh token endpoint
     * Handles both web and mobile clients
     */
    @Operation(summary = "Refresh authentication token", description = "Refreshes the authentication token using a valid refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token successfully refreshed",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(
            @RequestBody(required = false) RefreshTokenRequest refreshRequest,
            HttpServletRequest request,
            HttpServletResponse response) {

        // Get refresh token from cookie or request body
        String refreshToken = authenticationHelper.getRefreshTokenFromRequest(refreshRequest, request);
        if (refreshToken == null) {
            throw new AuthenticationException("Refresh token is required");
        }

        // Validate refresh token
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new AuthenticationException("Invalid refresh token");
        }

        // Check token type
        if (tokenProvider.getTokenType(refreshToken) != JwtTokenProvider.TokenType.REFRESH) {
            throw new AuthenticationException("Invalid token type");
        }

        // Get authentication from refresh token
        Authentication authentication = tokenProvider.getAuthentication(refreshToken);
        JwtTokenProvider.ClientType clientType = tokenProvider.getClientType(refreshToken);

        // Generate new token pair
        JwtTokenProvider.TokenPair tokenPair = tokenProvider.createTokenPair(authentication, clientType);

        // Handle response based on client type
        if (clientType == JwtTokenProvider.ClientType.WEB) {
            // Add tokens as HttpOnly cookies
            tokenProvider.addAccessTokenCookie(response, tokenPair.getAccessToken());
            tokenProvider.addRefreshTokenCookie(response, tokenPair.getRefreshToken());

            // Return response without tokens in body
            return ResponseEntity.ok(TokenResponse.builder()
                    .tokenType("Bearer")
                    .expiresIn(tokenProvider.getAccessTokenExpirationWeb() / 1000)
                    .username(((UserDetails) authentication.getPrincipal()).getUsername())
                    .message("Token refreshed successfully")
                    .build());
        } else {
            // Return tokens in response body for mobile
            return ResponseEntity.ok(TokenResponse.builder()
                    .accessToken(tokenPair.getAccessToken())
                    .refreshToken(tokenPair.getRefreshToken())
                    .tokenType("Bearer")
                    .expiresIn(tokenProvider.getAccessTokenExpirationMobile() / 1000)
                    .username(((UserDetails) authentication.getPrincipal()).getUsername())
                    .build());
        }
    }

    /**
     * Logout endpoint
     * Handles both web and mobile clients
     */
    @Operation(summary = "Logout", description = "Logs out the user by invalidating the session and clearing tokens")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully logged out",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TokenResponse.class)))
    })
    @PostMapping("/logout")
    public ResponseEntity<TokenResponse> logout(HttpServletRequest request, HttpServletResponse response) {
        // For web clients, clear cookies
        tokenProvider.clearTokenCookies(response);

        // Clear security context
        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(TokenResponse.builder()
                .message("Logged out successfully")
                .build());
    }


}
