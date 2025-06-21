package com.toubson.modulith.identity.api;

import com.toubson.modulith.identity.application.AuthenticationService;
import com.toubson.modulith.identity.config.JwtTokenProvider;
import com.toubson.modulith.identity.dto.LoginRequest;
import com.toubson.modulith.identity.dto.RefreshTokenRequest;
import com.toubson.modulith.identity.dto.TokenResponse;
import com.toubson.modulith.identity.exception.AuthenticationException;
import com.toubson.modulith.identity.utils.ClientTypeResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Authentication Controller Tests")
class AuthControllerTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private ClientTypeResolver clientTypeResolver;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private AuthController authController;

    private Authentication authentication;
    private LoginRequest loginRequest;
    private JwtTokenProvider.TokenPair tokenPair;

    @BeforeEach
    void setUp() {
        // Configuration des données de test
        UserDetails userDetails = new User(
                "testuser",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password");

        tokenPair = new JwtTokenProvider.TokenPair("access-token", "refresh-token");
    }

    @Test
    @DisplayName("Web login should return tokens in cookies when authentication is successful")
    void webLogin_shouldReturnTokensInCookies_whenAuthenticationSuccessful() {
        // Arrange
        when(authenticationService.authenticate(anyString(), anyString()))
                .thenReturn(authentication);
        when(tokenProvider.createTokenPair(any(Authentication.class), eq(JwtTokenProvider.ClientType.WEB)))
                .thenReturn(tokenPair);
        when(tokenProvider.getAccessTokenExpirationWeb()).thenReturn(3600000L);

        // Act
        ResponseEntity<TokenResponse> result = authController.webLogin(loginRequest, response);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("Bearer", result.getBody().getTokenType());
        assertEquals("testuser", result.getBody().getUsername());
        assertEquals("Authentication successful", result.getBody().getMessage());
        assertEquals(3600L, result.getBody().getExpiresIn());
        assertNull(result.getBody().getAccessToken());
        assertNull(result.getBody().getRefreshToken());

        // Vérification des appels
        verify(authenticationService).authenticate("testuser", "password");
        verify(tokenProvider).createTokenPair(authentication, JwtTokenProvider.ClientType.WEB);
        verify(tokenProvider).addAccessTokenCookie(response, "access-token");
        verify(tokenProvider).addRefreshTokenCookie(response, "refresh-token");
    }

    @Test
    @DisplayName("Web login should throw exception when authentication fails")
    void webLogin_shouldThrowException_whenAuthenticationFails() {
        // Arrange
        when(authenticationService.authenticate(anyString(), anyString()))
                .thenThrow(new AuthenticationException("Authentication failed"));

        // Act & Assert
        assertThrows(AuthenticationException.class, () -> authController.webLogin(loginRequest, response));

        verify(authenticationService).authenticate("testuser", "password");
        verify(tokenProvider, never()).createTokenPair(any(), any());
    }

    @Test
    @DisplayName("Mobile login should return tokens in response body when authentication is successful")
    void mobileLogin_shouldReturnTokensInBody_whenAuthenticationSuccessful() {
        // Arrange
        when(authenticationService.authenticate(anyString(), anyString()))
                .thenReturn(authentication);
        when(tokenProvider.createTokenPair(any(Authentication.class), eq(JwtTokenProvider.ClientType.MOBILE)))
                .thenReturn(tokenPair);
        when(tokenProvider.getAccessTokenExpirationMobile()).thenReturn(7200000L);

        // Act
        ResponseEntity<TokenResponse> result = authController.mobileLogin(loginRequest);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("Bearer", result.getBody().getTokenType());
        assertEquals("testuser", result.getBody().getUsername());
        assertEquals("access-token", result.getBody().getAccessToken());
        assertEquals("refresh-token", result.getBody().getRefreshToken());
        assertEquals(7200L, result.getBody().getExpiresIn());

        // Vérification des appels
        verify(authenticationService).authenticate("testuser", "password");
        verify(tokenProvider).createTokenPair(authentication, JwtTokenProvider.ClientType.MOBILE);
        verify(tokenProvider, never()).addAccessTokenCookie(any(), any());
        verify(tokenProvider, never()).addRefreshTokenCookie(any(), any());
    }

    @Test
    @DisplayName("Mobile login should throw exception when authentication fails")
    void mobileLogin_shouldThrowException_whenAuthenticationFails() {
        // Arrange
        when(authenticationService.authenticate(anyString(), anyString()))
                .thenThrow(new AuthenticationException("Authentication failed"));

        // Act & Assert
        assertThrows(AuthenticationException.class, () -> authController.mobileLogin(loginRequest));

        verify(authenticationService).authenticate("testuser", "password");
        verify(tokenProvider, never()).createTokenPair(any(), any());
    }

    @Test
    @DisplayName("Refresh token should refresh tokens for web client")
    void refreshToken_shouldRefreshTokens_forWebClient() {
        // Arrange
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken("refresh-token");

        when(authenticationService.getRefreshTokenFromRequest(any(), any()))
                .thenReturn("refresh-token");
        when(tokenProvider.validateToken("refresh-token")).thenReturn(true);
        when(tokenProvider.getTokenType("refresh-token"))
                .thenReturn(JwtTokenProvider.TokenType.REFRESH);
        when(tokenProvider.getAuthentication("refresh-token")).thenReturn(authentication);
        when(tokenProvider.getClientType("refresh-token"))
                .thenReturn(JwtTokenProvider.ClientType.WEB);
        when(tokenProvider.createTokenPair(authentication, JwtTokenProvider.ClientType.WEB))
                .thenReturn(tokenPair);
        when(tokenProvider.getAccessTokenExpirationWeb()).thenReturn(3600000L);

        // Act
        ResponseEntity<TokenResponse> result = authController.refreshToken(refreshRequest, request, response);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("Bearer", result.getBody().getTokenType());
        assertEquals("testuser", result.getBody().getUsername());
        assertEquals("Token refreshed successfully", result.getBody().getMessage());
        assertNull(result.getBody().getAccessToken());
        assertNull(result.getBody().getRefreshToken());

        // Vérification des appels
        verify(tokenProvider).addAccessTokenCookie(response, "access-token");
        verify(tokenProvider).addRefreshTokenCookie(response, "refresh-token");
    }

    @Test
    @DisplayName("Refresh token should refresh tokens for mobile client")
    void refreshToken_shouldRefreshTokens_forMobileClient() {
        // Arrange
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken("refresh-token");

        when(authenticationService.getRefreshTokenFromRequest(any(), any()))
                .thenReturn("refresh-token");
        when(tokenProvider.validateToken("refresh-token")).thenReturn(true);
        when(tokenProvider.getTokenType("refresh-token"))
                .thenReturn(JwtTokenProvider.TokenType.REFRESH);
        when(tokenProvider.getAuthentication("refresh-token")).thenReturn(authentication);
        when(tokenProvider.getClientType("refresh-token"))
                .thenReturn(JwtTokenProvider.ClientType.MOBILE);
        when(tokenProvider.createTokenPair(authentication, JwtTokenProvider.ClientType.MOBILE))
                .thenReturn(tokenPair);
        when(tokenProvider.getAccessTokenExpirationMobile()).thenReturn(7200000L);

        // Act
        ResponseEntity<TokenResponse> result = authController.refreshToken(refreshRequest, request, response);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("Bearer", result.getBody().getTokenType());
        assertEquals("testuser", result.getBody().getUsername());
        assertEquals("access-token", result.getBody().getAccessToken());
        assertEquals("refresh-token", result.getBody().getRefreshToken());
        assertEquals(7200L, result.getBody().getExpiresIn());

        // Vérification que les cookies ne sont pas utilisés pour mobile
        verify(tokenProvider, never()).addAccessTokenCookie(any(), any());
        verify(tokenProvider, never()).addRefreshTokenCookie(any(), any());
    }

    @Test
    @DisplayName("Refresh token should throw exception when token is missing")
    void refreshToken_shouldThrowException_whenTokenMissing() {
        // Arrange
        when(authenticationService.getRefreshTokenFromRequest(any(), any()))
                .thenReturn(null);

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> authController.refreshToken(null, request, response));

        assertEquals("Refresh token is required", exception.getMessage());
        verify(tokenProvider, never()).validateToken(any());
    }

    @Test
    @DisplayName("Refresh token should throw exception when token is invalid")
    void refreshToken_shouldThrowException_whenTokenInvalid() {
        // Arrange
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken("invalid-token");

        when(authenticationService.getRefreshTokenFromRequest(any(), any()))
                .thenReturn("invalid-token");
        when(tokenProvider.validateToken("invalid-token")).thenReturn(false);

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> authController.refreshToken(refreshRequest, request, response));

        assertEquals("Invalid refresh token", exception.getMessage());
        verify(tokenProvider, never()).getTokenType(any());
    }

    @Test
    @DisplayName("Refresh token should throw exception when token type is incorrect")
    void refreshToken_shouldThrowException_whenTokenTypeIncorrect() {
        // Arrange
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken("access-token");

        when(authenticationService.getRefreshTokenFromRequest(any(), any()))
                .thenReturn("access-token");
        when(tokenProvider.validateToken("access-token")).thenReturn(true);
        when(tokenProvider.getTokenType("access-token"))
                .thenReturn(JwtTokenProvider.TokenType.ACCESS);

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> authController.refreshToken(refreshRequest, request, response));

        assertEquals("Invalid token type", exception.getMessage());
        verify(tokenProvider, never()).getAuthentication(any());
    }

    @Test
    @DisplayName("Logout should clear cookies and security context")
    void logout_shouldClearCookiesAndContext() {
        // Arrange
        when(clientTypeResolver.resolveClientType(request))
                .thenReturn(JwtTokenProvider.ClientType.WEB);

        // Act
        ResponseEntity<TokenResponse> result = authController.logout(request, response);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("Logged out successfully", result.getBody().getMessage());

        // Vérification des appels
        verify(tokenProvider).clearTokenCookies(response);
        verify(clientTypeResolver).resolveClientType(request);
    }

    @Test
    @DisplayName("Logout should handle mobile client correctly")
    void logout_shouldHandleMobileClient() {
        // Arrange
        when(clientTypeResolver.resolveClientType(request))
                .thenReturn(JwtTokenProvider.ClientType.MOBILE);

        // Act
        ResponseEntity<TokenResponse> result = authController.logout(request, response);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("Logged out successfully", result.getBody().getMessage());

        // Vérification que les cookies sont quand même vidés (au cas où)
        verify(tokenProvider).clearTokenCookies(response);
    }
}