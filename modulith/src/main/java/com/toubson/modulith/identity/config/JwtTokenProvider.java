package com.toubson.modulith.identity.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Getter
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token.expiration.web}")
    private long accessTokenExpirationWeb;

    @Value("${jwt.access-token.expiration.mobile}")
    private long accessTokenExpirationMobile;

    @Value("${jwt.refresh-token.expiration.web}")
    private long refreshTokenExpirationWeb;

    @Value("${jwt.refresh-token.expiration.mobile}")
    private long refreshTokenExpirationMobile;

    private Key key;

    @PostConstruct
    protected void init() {
        // Use HMAC-SHA algorithm with the secret key
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    /**
     * Creates an access token for the given authentication and client type
     */
    public String createAccessToken(Authentication authentication, ClientType clientType) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return createToken(userDetails, clientType, TokenType.ACCESS);
    }

    /**
     * Creates a refresh token for the given authentication and client type
     */
    public String createRefreshToken(Authentication authentication, ClientType clientType) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return createToken(userDetails, clientType, TokenType.REFRESH);
    }

    /**
     * Creates a token pair (access and refresh) for the given authentication and client type
     */
    public TokenPair createTokenPair(Authentication authentication, ClientType clientType) {
        String accessToken = createAccessToken(authentication, clientType);
        String refreshToken = createRefreshToken(authentication, clientType);
        return new TokenPair(accessToken, refreshToken);
    }

    /**
     * Adds the access token as an HttpOnly cookie to the response (for web clients)
     */
    public void addAccessTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("access_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // Requires HTTPS
        cookie.setPath("/");
        cookie.setMaxAge((int) (accessTokenExpirationWeb / 1000)); // Convert to seconds
        response.addCookie(cookie);
    }

    /**
     * Adds the refresh token as an HttpOnly cookie to the response (for web clients)
     */
    public void addRefreshTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("refresh_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // Requires HTTPS
        cookie.setPath("/auth/refresh"); // Only sent to refresh endpoint
        cookie.setMaxAge((int) (refreshTokenExpirationWeb / 1000)); // Convert to seconds
        response.addCookie(cookie);
    }

    /**
     * Clears the token cookies (for logout)
     */
    public void clearTokenCookies(HttpServletResponse response) {
        Cookie accessCookie = new Cookie("access_token", "");
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie("refresh_token", "");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/auth/refresh");
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);
    }

    private String createToken(UserDetails userDetails, ClientType clientType, TokenType tokenType) {
        String username = userDetails.getUsername();
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

        Claims claims = Jwts.claims().setSubject(username);
        claims.put("roles", authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(",")));
        claims.put("clientType", clientType.name());
        claims.put("tokenType", tokenType.name());

        Date now = new Date();
        Date validity = new Date(now.getTime() + getExpirationTime(clientType, tokenType));

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private long getExpirationTime(ClientType clientType, TokenType tokenType) {
        if (tokenType == TokenType.ACCESS) {
            return clientType == ClientType.WEB ? accessTokenExpirationWeb : accessTokenExpirationMobile;
        } else {
            return clientType == ClientType.WEB ? refreshTokenExpirationWeb : refreshTokenExpirationMobile;
        }
    }

    public Authentication getAuthentication(String token) {
        Claims claims = extractClaims(token);

        String username = claims.getSubject();
        Collection<? extends GrantedAuthority> authorities = Arrays.stream(claims.get("roles", String.class).split(","))
                .filter(role -> !role.isEmpty())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        UserDetails userDetails = new User(username, "", authorities);
        return new UsernamePasswordAuthenticationToken(userDetails, "", authorities);
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = extractClaims(token);
            Date expiration = claims.getExpiration();
            return !expiration.before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public ClientType getClientType(String token) {
        Claims claims = extractClaims(token);
        return ClientType.valueOf(claims.get("clientType", String.class));
    }

    public TokenType getTokenType(String token) {
        Claims claims = extractClaims(token);
        return TokenType.valueOf(claims.get("tokenType", String.class));
    }

    private Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public enum ClientType {
        WEB, MOBILE
    }

    public enum TokenType {
        ACCESS, REFRESH
    }

    @Getter
    public static class TokenPair {
        private final String accessToken;
        private final String refreshToken;

        public TokenPair(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }
}
