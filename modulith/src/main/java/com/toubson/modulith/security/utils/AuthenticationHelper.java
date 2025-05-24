package com.toubson.modulith.security.utils;

import com.toubson.modulith.security.dto.RefreshTokenRequest;
import com.toubson.modulith.security.exception.AuthenticationException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthenticationHelper {

    private final AuthenticationManager authenticationManager;

    /**
     * Helper method to authenticate a user
     */
    public Authentication authenticate(String username, String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return authentication;
        } catch (Exception e) {
            throw new AuthenticationException("Authentication failed: " + e.getMessage(), e);
        }
    }

    /**
     * Helper method to get refresh token from request
     */
    public String getRefreshTokenFromRequest(RefreshTokenRequest refreshRequest, HttpServletRequest request) {
        // First check request body (for mobile clients)
        if (refreshRequest != null && refreshRequest.getRefreshToken() != null) {
            return refreshRequest.getRefreshToken();
        }

        // Then check cookies (for web clients)
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            Optional<Cookie> refreshTokenCookie = Arrays.stream(cookies)
                    .filter(cookie -> "refresh_token".equals(cookie.getName()))
                    .findFirst();

            if (refreshTokenCookie.isPresent()) {
                return refreshTokenCookie.get().getValue();
            }
        }

        return null;
    }
}
