package com.flightreservation.service;

import com.flightreservation.dto.request.LoginRequest;
import com.flightreservation.dto.request.RegisterRequest;
import com.flightreservation.dto.response.AuthResponse;
import com.flightreservation.entity.AuditLog;
import com.flightreservation.entity.User;
import com.flightreservation.exception.ApiException;
import com.flightreservation.repository.AuditLogRepository;
import com.flightreservation.repository.UserRepository;
import com.flightreservation.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("encoded_pass")
                .phoneNumber("1234567890")
                .role(User.Role.PASSENGER)
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("register - successfully registers a new user")
    void register_Success() {
        RegisterRequest req = new RegisterRequest();
        req.setFirstName("John");
        req.setLastName("Doe");
        req.setEmail("john@example.com");
        req.setPassword("password123");
        req.setPhoneNumber("1234567890");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_pass");
        when(jwtTokenProvider.generateAccessToken("john@example.com")).thenReturn("access_token");
        when(jwtTokenProvider.generateRefreshToken("john@example.com")).thenReturn("refresh_token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse resp = authService.register(req);

        assertNotNull(resp);
        assertEquals("access_token", resp.getAccessToken());
        assertEquals("refresh_token", resp.getRefreshToken());
        assertEquals("john@example.com", resp.getUser().getEmail());
        verify(userRepository, times(1)).save(any(User.class));
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("register - throws conflict when email already exists")
    void register_DuplicateEmail_ThrowsConflict() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("john@example.com");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(ApiException.class, () -> authService.register(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login - successfully authenticates user and generates tokens")
    void login_Success() {
        LoginRequest req = new LoginRequest();
        req.setEmail("john@example.com");
        req.setPassword("password123");

        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateAccessToken(auth)).thenReturn("access_token");
        when(jwtTokenProvider.generateRefreshToken("john@example.com")).thenReturn("refresh_token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse resp = authService.login(req);

        assertNotNull(resp);
        assertEquals("access_token", resp.getAccessToken());
        assertEquals("refresh_token", resp.getRefreshToken());
        assertEquals("john@example.com", resp.getUser().getEmail());
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("login - throws BadCredentialsException when password is wrong")
    void login_BadCredentials_ThrowsException() {
        LoginRequest req = new LoginRequest();
        req.setEmail("john@example.com");
        req.setPassword("wrong");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(req));
    }

    @Test
    @DisplayName("refreshToken - generates new tokens when refresh token is valid")
    void refreshToken_Success() {
        when(jwtTokenProvider.validateToken("valid_refresh")).thenReturn(true);
        when(jwtTokenProvider.extractEmail("valid_refresh")).thenReturn("john@example.com");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateAccessToken("john@example.com")).thenReturn("new_access");
        when(jwtTokenProvider.generateRefreshToken("john@example.com")).thenReturn("new_refresh");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse resp = authService.refreshToken("valid_refresh");

        assertNotNull(resp);
        assertEquals("new_access", resp.getAccessToken());
        assertEquals("new_refresh", resp.getRefreshToken());
    }

    @Test
    @DisplayName("refreshToken - throws bad request when token is invalid")
    void refreshToken_InvalidToken_ThrowsBadRequest() {
        when(jwtTokenProvider.validateToken("invalid_refresh")).thenReturn(false);

        assertThrows(ApiException.class, () -> authService.refreshToken("invalid_refresh"));
    }
}

