package com.flightreservation.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        jwtProperties.setExpirationMs(3600000L); // 1 hour
        jwtProperties.setRefreshExpirationMs(7200000L); // 2 hours

        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
    }

    @Test
    @DisplayName("generateAccessToken from email - generates valid token and extracts email")
    void generateAccessToken_FromEmail_ValidatesAndExtracts() {
        String token = jwtTokenProvider.generateAccessToken("test@example.com");

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("test@example.com", jwtTokenProvider.extractEmail(token));
        assertEquals(3600000L, jwtTokenProvider.getExpirationMs());
    }

    @Test
    @DisplayName("generateAccessToken from Authentication - generates valid token")
    void generateAccessToken_FromAuth_Validates() {
        UserDetails userDetails = new User("admin@example.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        String token = jwtTokenProvider.generateAccessToken(auth);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("admin@example.com", jwtTokenProvider.extractEmail(token));
    }

    @Test
    @DisplayName("generateRefreshToken - generates valid refresh token")
    void generateRefreshToken_Success() {
        String refreshToken = jwtTokenProvider.generateRefreshToken("test@example.com");

        assertNotNull(refreshToken);
        assertTrue(jwtTokenProvider.validateToken(refreshToken));
        assertEquals("test@example.com", jwtTokenProvider.extractEmail(refreshToken));
    }

    @Test
    @DisplayName("validateToken - returns false for invalid token")
    void validateToken_Invalid_ReturnsFalse() {
        assertFalse(jwtTokenProvider.validateToken("invalid.token.here"));
    }
}
