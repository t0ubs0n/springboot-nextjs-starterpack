package com.toubson.modulith.identity.application;

import com.toubson.modulith.identity.dto.RefreshTokenRequest;
import com.toubson.modulith.identity.exception.AuthenticationException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;

    /**
     * Authenticates a user with username and password
     * Non-recursive version to avoid AOP issues
     */
    public Authentication authenticate(String username, String password) {
        log.debug("Authentication attempt for user: {}", username);

        try {
            // Create authentication token
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(username, password);

            // Perform authentication via manager
            Authentication authentication = authenticationManager.authenticate(authToken);

            // Set security context
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("Authentication successful for user: {}", username);
            return authentication;

        } catch (org.springframework.security.core.AuthenticationException springAuthEx) {
            log.warn("Authentication failed for user: {} - Reason: {}",
                    username, springAuthEx.getMessage());
            throw new AuthenticationException("Authentication failed: " + springAuthEx.getMessage(), springAuthEx);
        } catch (Exception e) {
            log.error("Unexpected error during authentication for user: {}", username, e);
            throw new AuthenticationException("Authentication failed due to unexpected error: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts the refresh token from the request
     */
    public String getRefreshTokenFromRequest(RefreshTokenRequest refreshRequest, HttpServletRequest request) {
        log.debug("Extracting refresh token from request");

        // First check request body (for mobile clients)
        if (refreshRequest != null && refreshRequest.getRefreshToken() != null) {
            log.debug("Refresh token found in request body");
            return refreshRequest.getRefreshToken();
        }

        // Then check cookies (for web clients)
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            Optional<Cookie> refreshTokenCookie = Arrays.stream(cookies)
                    .filter(cookie -> "refresh_token".equals(cookie.getName()))
                    .findFirst();

            if (refreshTokenCookie.isPresent()) {
                log.debug("Refresh token found in cookies");
                return refreshTokenCookie.get().getValue();
            }
        }

        log.debug("No refresh token found");
        return null;
    }
}
