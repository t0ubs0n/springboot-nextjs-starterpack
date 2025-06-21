package com.toubson.modulith.identity.utils;

import com.toubson.modulith.identity.config.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Utility class to detect the client type (web or mobile) from the request
 */
@Component
public class ClientTypeResolver {

    private static final String CLIENT_TYPE_HEADER = "X-Client-Type";
    private static final String USER_AGENT_HEADER = "User-Agent";

    /**
     * Detects the client type from the request
     *
     * @param request The HTTP request
     * @return The client type (WEB or MOBILE)
     */
    public JwtTokenProvider.ClientType resolveClientType(HttpServletRequest request) {
        // First check if the client type is explicitly specified in a header
        String clientTypeHeader = request.getHeader(CLIENT_TYPE_HEADER);
        if (StringUtils.hasText(clientTypeHeader)) {
            try {
                return JwtTokenProvider.ClientType.valueOf(clientTypeHeader.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid client type, fall back to User-Agent detection
            }
        }

        // Otherwise, try to detect from User-Agent
        String userAgent = request.getHeader(USER_AGENT_HEADER);
        if (StringUtils.hasText(userAgent)) {
            if (isMobileUserAgent(userAgent)) {
                return JwtTokenProvider.ClientType.MOBILE;
            }
        }

        // Default to web client
        return JwtTokenProvider.ClientType.WEB;
    }

    /**
     * Checks if the User-Agent string indicates a mobile client
     *
     * @param userAgent The User-Agent header value
     * @return true if the User-Agent indicates a mobile client, false otherwise
     */
    private boolean isMobileUserAgent(String userAgent) {
        // Common mobile platform keywords
        String[] mobileKeywords = {
                "Android", "iPhone", "iPad", "iPod", "BlackBerry", "IEMobile", "Opera Mini",
                "Windows Phone", "webOS", "Mobile", "mobile"
        };

        for (String keyword : mobileKeywords) {
            if (userAgent.contains(keyword)) {
                return true;
            }
        }

        return false;
    }
}