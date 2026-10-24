package com.flightreservation.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AuditingConfigTest {

    private final AuditingConfig auditingConfig = new AuditingConfig();
    private final AuditorAware<String> auditorAware = auditingConfig.auditorProvider();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("auditorProvider - returns 'system' when security context authentication is null")
    void auditorProvider_NullAuth_ReturnsSystem() {
        SecurityContextHolder.clearContext();

        Optional<String> auditor = auditorAware.getCurrentAuditor();

        assertTrue(auditor.isPresent());
        assertEquals("system", auditor.get());
    }

    @Test
    @DisplayName("auditorProvider - returns 'system' when authentication is not authenticated")
    void auditorProvider_Unauthenticated_ReturnsSystem() {
        UsernamePasswordAuthenticationToken unauth = new UsernamePasswordAuthenticationToken("user", "pass");
        unauth.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(unauth);

        Optional<String> auditor = auditorAware.getCurrentAuditor();

        assertTrue(auditor.isPresent());
        assertEquals("system", auditor.get());
    }

    @Test
    @DisplayName("auditorProvider - returns 'system' when authentication name is 'anonymousUser'")
    void auditorProvider_AnonymousUser_ReturnsSystem() {
        AnonymousAuthenticationToken anonAuth = new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anonAuth);

        Optional<String> auditor = auditorAware.getCurrentAuditor();

        assertTrue(auditor.isPresent());
        assertEquals("system", auditor.get());
    }

    @Test
    @DisplayName("auditorProvider - returns username when user is authenticated")
    void auditorProvider_AuthenticatedUser_ReturnsUsername() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "john@example.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_PASSENGER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<String> auditor = auditorAware.getCurrentAuditor();

        assertTrue(auditor.isPresent());
        assertEquals("john@example.com", auditor.get());
    }
}

