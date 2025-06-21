package com.toubson.modulith.identity.api;

import com.toubson.modulith.identity.application.AuthenticationService;
import com.toubson.modulith.identity.config.JwtTokenProvider;
import com.toubson.modulith.identity.dto.LoginRequest;
import com.toubson.modulith.identity.dto.RefreshTokenRequest;
import com.toubson.modulith.identity.dto.TokenResponse;
import com.toubson.modulith.identity.exception.AuthenticationException;
import com.toubson.modulith.identity.utils.ClientTypeResolver;
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
    private final AuthenticationService authenticationService;

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
        log.debug("Processing web login request for username: {}", loginRequest.getUsername());

        Authentication authentication = authenticationService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());
        log.debug("Authentication successful for web user: {}", loginRequest.getUsername());

        JwtTokenProvider.TokenPair tokenPair = tokenProvider.createTokenPair(
                authentication, JwtTokenProvider.ClientType.WEB);
        log.debug("JWT token pair created for web user");

        // Add tokens as HttpOnly cookies
        log.debug("Adding access and refresh tokens as HttpOnly cookies");
        tokenProvider.addAccessTokenCookie(response, tokenPair.getAccessToken());
        tokenProvider.addRefreshTokenCookie(response, tokenPair.getRefreshToken());

        // Return response without tokens in body (they're in cookies)
        log.debug("Web login completed successfully for user: {}", loginRequest.getUsername());
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
        log.debug("Processing mobile login request for username: {}", loginRequest.getUsername());

        Authentication authentication = authenticationService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());
        log.debug("Authentication successful for mobile user: {}", loginRequest.getUsername());

        JwtTokenProvider.TokenPair tokenPair = tokenProvider.createTokenPair(
                authentication, JwtTokenProvider.ClientType.MOBILE);
        log.debug("JWT token pair created for mobile user");

        // Return tokens in response body
        log.debug("Mobile login completed successfully for user: {}", loginRequest.getUsername());
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
        log.debug("Processing token refresh request");

        // Get refresh token from cookie or request body
        String refreshToken = authenticationService.getRefreshTokenFromRequest(refreshRequest, request);
        if (refreshToken == null) {
            log.debug("Refresh token is missing in the request");
            throw new AuthenticationException("Refresh token is required");
        }
        log.debug("Refresh token extracted from request");

        // Validate refresh token
        if (!tokenProvider.validateToken(refreshToken)) {
            log.debug("Refresh token validation failed");
            throw new AuthenticationException("Invalid refresh token");
        }
        log.debug("Refresh token validated successfully");

        // Check token type
        if (tokenProvider.getTokenType(refreshToken) != JwtTokenProvider.TokenType.REFRESH) {
            log.debug("Invalid token type - expected REFRESH token");
            throw new AuthenticationException("Invalid token type");
        }
        log.debug("Token type verified as REFRESH");

        // Get authentication from refresh token
        Authentication authentication = tokenProvider.getAuthentication(refreshToken);
        String username = ((UserDetails) authentication.getPrincipal()).getUsername();
        log.debug("Authentication extracted from refresh token for user: {}", username);

        JwtTokenProvider.ClientType clientType = tokenProvider.getClientType(refreshToken);
        log.debug("Client type determined: {}", clientType);

        // Generate new token pair
        JwtTokenProvider.TokenPair tokenPair = tokenProvider.createTokenPair(authentication, clientType);
        log.debug("New token pair created for user: {}", username);

        // Handle response based on client type
        if (clientType == JwtTokenProvider.ClientType.WEB) {
            // Add tokens as HttpOnly cookies
            log.debug("Adding new tokens as HttpOnly cookies for web client");
            tokenProvider.addAccessTokenCookie(response, tokenPair.getAccessToken());
            tokenProvider.addRefreshTokenCookie(response, tokenPair.getRefreshToken());

            // Return response without tokens in body
            log.debug("Token refresh completed successfully for web user: {}", username);
            return ResponseEntity.ok(TokenResponse.builder()
                    .tokenType("Bearer")
                    .expiresIn(tokenProvider.getAccessTokenExpirationWeb() / 1000)
                    .username(username)
                    .message("Token refreshed successfully")
                    .build());
        } else {
            // Return tokens in response body for mobile
            log.debug("Token refresh completed successfully for mobile user: {}", username);
            return ResponseEntity.ok(TokenResponse.builder()
                    .accessToken(tokenPair.getAccessToken())
                    .refreshToken(tokenPair.getRefreshToken())
                    .tokenType("Bearer")
                    .expiresIn(tokenProvider.getAccessTokenExpirationMobile() / 1000)
                    .username(username)
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
        log.debug("Processing logout request");

        // Determine client type for better logging
        String clientType = clientTypeResolver.resolveClientType(request).toString();
        log.debug("Client type for logout: {}", clientType);

        // For web clients, clear cookies
        log.debug("Clearing token cookies from response");
        tokenProvider.clearTokenCookies(response);

        // Clear security context
        log.debug("Clearing security context");
        SecurityContextHolder.clearContext();

        log.debug("Logout completed successfully");
        return ResponseEntity.ok(TokenResponse.builder()
                .message("Logged out successfully")
                .build());
    }


}
