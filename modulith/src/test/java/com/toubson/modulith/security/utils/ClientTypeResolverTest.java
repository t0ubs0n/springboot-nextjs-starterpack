package com.toubson.modulith.security.utils;

import com.toubson.modulith.security.config.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientTypeResolverTest {

    private ClientTypeResolver clientTypeResolver;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        clientTypeResolver = new ClientTypeResolver();
        request = new MockHttpServletRequest();
    }

    @Test
    void resolveClientType_shouldReturnWeb_whenNoHeadersPresent() {
        // When
        JwtTokenProvider.ClientType clientType = clientTypeResolver.resolveClientType(request);

        // Then
        assertEquals(JwtTokenProvider.ClientType.WEB, clientType);
    }

    @Test
    void resolveClientType_shouldReturnWeb_whenExplicitlySpecified() {
        // Given
        request.addHeader("X-Client-Type", "WEB");

        // When
        JwtTokenProvider.ClientType clientType = clientTypeResolver.resolveClientType(request);

        // Then
        assertEquals(JwtTokenProvider.ClientType.WEB, clientType);
    }

    @Test
    void resolveClientType_shouldReturnMobile_whenExplicitlySpecified() {
        // Given
        request.addHeader("X-Client-Type", "MOBILE");

        // When
        JwtTokenProvider.ClientType clientType = clientTypeResolver.resolveClientType(request);

        // Then
        assertEquals(JwtTokenProvider.ClientType.MOBILE, clientType);
    }

    @Test
    void resolveClientType_shouldReturnWeb_whenInvalidClientTypeSpecified() {
        // Given
        request.addHeader("X-Client-Type", "INVALID");

        // When
        JwtTokenProvider.ClientType clientType = clientTypeResolver.resolveClientType(request);

        // Then
        assertEquals(JwtTokenProvider.ClientType.WEB, clientType);
    }

    @Test
    void resolveClientType_shouldReturnMobile_whenAndroidUserAgent() {
        // Given
        request.addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; SM-G973F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36");

        // When
        JwtTokenProvider.ClientType clientType = clientTypeResolver.resolveClientType(request);

        // Then
        assertEquals(JwtTokenProvider.ClientType.MOBILE, clientType);
    }

    @Test
    void resolveClientType_shouldReturnMobile_whenIPhoneUserAgent() {
        // Given
        request.addHeader("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1");

        // When
        JwtTokenProvider.ClientType clientType = clientTypeResolver.resolveClientType(request);

        // Then
        assertEquals(JwtTokenProvider.ClientType.MOBILE, clientType);
    }

    @Test
    void resolveClientType_shouldReturnWeb_whenDesktopUserAgent() {
        // Given
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

        // When
        JwtTokenProvider.ClientType clientType = clientTypeResolver.resolveClientType(request);

        // Then
        assertEquals(JwtTokenProvider.ClientType.WEB, clientType);
    }

    @Test
    void resolveClientType_shouldPrioritizeHeader_overUserAgent() {
        // Given
        request.addHeader("X-Client-Type", "WEB");
        request.addHeader("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1");

        // When
        JwtTokenProvider.ClientType clientType = clientTypeResolver.resolveClientType(request);

        // Then
        assertEquals(JwtTokenProvider.ClientType.WEB, clientType);
    }
}