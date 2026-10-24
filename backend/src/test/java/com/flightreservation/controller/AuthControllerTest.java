package com.flightreservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightreservation.dto.request.LoginRequest;
import com.flightreservation.dto.request.RegisterRequest;
import com.flightreservation.dto.response.AuthResponse;
import com.flightreservation.exception.ApiException;
import com.flightreservation.exception.GlobalExceptionHandler;
import com.flightreservation.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UserDetails mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User("passenger@example.com", "password",
                List.of(new SimpleGrantedAuthority("ROLE_PASSENGER")));

        HandlerMethodArgumentResolver userDetailsResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().isAssignableFrom(UserDetails.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                return mockUser;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(userDetailsResolver)
                .build();
    }

    private AuthResponse createMockAuthResponse(String email) {
        return AuthResponse.builder()
                .accessToken("jwt-token-xyz")
                .refreshToken("refresh-token-xyz")
                .tokenType("Bearer")
                .expiresIn(86400L)
                .user(AuthResponse.UserInfo.builder()
                        .id(1L)
                        .email(email)
                        .firstName("John")
                        .lastName("Doe")
                        .role("ROLE_PASSENGER")
                        .build())
                .build();
    }

    @Nested
    @DisplayName("POST /auth/register")
    class RegisterTests {

        @Test
        @DisplayName("Success - returns 201 Created with tokens and user info")
        void register_Success() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setFirstName("John");
            req.setLastName("Doe");
            req.setEmail("john.doe@example.com");
            req.setPassword("password123");
            req.setPhoneNumber("1234567890");

            when(authService.register(any(RegisterRequest.class)))
                    .thenReturn(createMockAuthResponse("john.doe@example.com"));

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accessToken", is("jwt-token-xyz")))
                    .andExpect(jsonPath("$.refreshToken", is("refresh-token-xyz")))
                    .andExpect(jsonPath("$.user.email", is("john.doe@example.com")))
                    .andExpect(jsonPath("$.user.role", is("ROLE_PASSENGER")));

            verify(authService, times(1)).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("Validation Error - missing required fields returns 400 Bad Request")
        void register_MissingRequiredFields_Returns400() throws Exception {
            RegisterRequest req = new RegisterRequest();

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", is("Validation failed")))
                    .andExpect(jsonPath("$.validationErrors.firstName", notNullValue()))
                    .andExpect(jsonPath("$.validationErrors.lastName", notNullValue()))
                    .andExpect(jsonPath("$.validationErrors.email", notNullValue()))
                    .andExpect(jsonPath("$.validationErrors.password", notNullValue()));

            verify(authService, never()).register(any());
        }

        @Test
        @DisplayName("Validation Error - invalid email format returns 400")
        void register_InvalidEmail_Returns400() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setFirstName("John");
            req.setLastName("Doe");
            req.setEmail("not-an-email");
            req.setPassword("password123");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.email", is("Invalid email format")));

            verify(authService, never()).register(any());
        }

        @Test
        @DisplayName("Validation Error - password less than 8 chars returns 400")
        void register_PasswordTooShort_Returns400() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setFirstName("John");
            req.setLastName("Doe");
            req.setEmail("valid@example.com");
            req.setPassword("short");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.password", is("Password must be at least 8 characters")));

            verify(authService, never()).register(any());
        }

        @Test
        @DisplayName("Conflict - duplicate email returns 409 Conflict")
        void register_DuplicateEmail_Returns409() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setFirstName("John");
            req.setLastName("Doe");
            req.setEmail("duplicate@example.com");
            req.setPassword("password123");

            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(ApiException.conflict("Email is already registered"));

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.message", is("Email is already registered")));

            verify(authService, times(1)).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("Malformed Request - invalid JSON body returns 400/500")
        void register_MalformedJson_ReturnsError() throws Exception {
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid-json}"))
                    .andExpect(status().is5xxServerError());
        }
    }

    @Nested
    @DisplayName("POST /auth/login")
    class LoginTests {

        @Test
        @DisplayName("Success - valid credentials return 200 OK with tokens")
        void login_Success() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setEmail("passenger@example.com");
            req.setPassword("password123");

            when(authService.login(any(LoginRequest.class)))
                    .thenReturn(createMockAuthResponse("passenger@example.com"));

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken", is("jwt-token-xyz")))
                    .andExpect(jsonPath("$.user.email", is("passenger@example.com")));

            verify(authService, times(1)).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("Validation Error - blank email and password returns 400")
        void login_MissingFields_Returns400() throws Exception {
            LoginRequest req = new LoginRequest();

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors.email", notNullValue()))
                    .andExpect(jsonPath("$.validationErrors.password", notNullValue()));

            verify(authService, never()).login(any());
        }

        @Test
        @DisplayName("Authentication Error - bad credentials returns 401 Unauthorized")
        void login_BadCredentials_Returns401() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setEmail("passenger@example.com");
            req.setPassword("wrong-password");

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status", is(401)))
                    .andExpect(jsonPath("$.message", is("Invalid credentials")));
        }
    }

    @Nested
    @DisplayName("POST /auth/refresh")
    class RefreshTokenTests {

        @Test
        @DisplayName("Success - valid refresh token returns 200 OK and new tokens")
        void refresh_Success() throws Exception {
            Map<String, String> body = Map.of("refreshToken", "valid-refresh-token");

            when(authService.refreshToken("valid-refresh-token"))
                    .thenReturn(createMockAuthResponse("passenger@example.com"));

            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken", is("jwt-token-xyz")))
                    .andExpect(jsonPath("$.refreshToken", is("refresh-token-xyz")));

            verify(authService, times(1)).refreshToken("valid-refresh-token");
        }

        @Test
        @DisplayName("Failure - expired or invalid refresh token returns 401 Unauthorized")
        void refresh_InvalidToken_Returns401() throws Exception {
            Map<String, String> body = Map.of("refreshToken", "expired-token");

            when(authService.refreshToken("expired-token"))
                    .thenThrow(new ApiException("Invalid or expired refresh token", HttpStatus.UNAUTHORIZED));

            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status", is(401)))
                    .andExpect(jsonPath("$.message", is("Invalid or expired refresh token")));
        }
    }

    @Nested
    @DisplayName("GET /auth/me")
    class MeTests {

        @Test
        @DisplayName("Success - returns current user details")
        void me_Success() throws Exception {
            mockMvc.perform(get("/auth/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email", is("passenger@example.com")))
                    .andExpect(jsonPath("$.authorities[0].authority", is("ROLE_PASSENGER")));
        }
    }
}

