package com.flightreservation.security;

import com.flightreservation.entity.User;
import com.flightreservation.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("loadUserByUsername - returns UserDetails when user exists")
    void loadUserByUsername_Success() {
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .password("encoded_pass")
                .role(User.Role.PASSENGER)
                .enabled(true)
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("user@example.com");

        assertNotNull(userDetails);
        assertEquals("user@example.com", userDetails.getUsername());
        assertEquals("encoded_pass", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PASSENGER")));
    }

    @Test
    @DisplayName("loadUserByUsername - throws UsernameNotFoundException when user does not exist")
    void loadUserByUsername_NotFound_ThrowsException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("unknown@example.com"));
    }

    @Test
    @DisplayName("loadUserByUsername - disabled user returns UserDetails with isEnabled false")
    void loadUserByUsername_DisabledUser() {
        User user = User.builder()
                .id(2L)
                .email("disabled@example.com")
                .password("encoded_pass")
                .role(User.Role.PASSENGER)
                .enabled(false)
                .build();

        when(userRepository.findByEmail("disabled@example.com")).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("disabled@example.com");

        assertNotNull(userDetails);
        assertFalse(userDetails.isEnabled());
    }
}

