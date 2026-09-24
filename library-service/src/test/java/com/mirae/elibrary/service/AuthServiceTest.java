package com.mirae.elibrary.service;

import com.mirae.elibrary.domain.Role;
import com.mirae.elibrary.domain.User;
import com.mirae.elibrary.exception.BusinessRuleException;
import com.mirae.elibrary.repository.UserRepository;
import com.mirae.elibrary.security.JwtService;
import com.mirae.elibrary.web.dto.AuthResponse;
import com.mirae.elibrary.web.dto.LoginRequest;
import com.mirae.elibrary.web.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager, jwtService);
    }

    @Test
    void register_success_hashesPasswordAndReturnsToken() {
        RegisterRequest request = new RegisterRequest("New@Demo.io", "password123", "New User");
        when(userRepository.existsByEmail("new@demo.io")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("new@demo.io"); // normalized to lowercase
        assertThat(saved.getPasswordHash()).isEqualTo("hashed"); // never the raw password
        assertThat(saved.getRole()).isEqualTo(Role.MEMBER);
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.email()).isEqualTo("new@demo.io");
    }

    @Test
    void register_duplicateEmail_throwsBusinessRule() {
        RegisterRequest request = new RegisterRequest("dupe@demo.io", "password123", "Dupe");
        when(userRepository.existsByEmail("dupe@demo.io")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already registered");
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_success_returnsToken() {
        LoginRequest request = new LoginRequest("member@demo.io", "password123");
        User user = new User("member@demo.io", "hash", "Member", Role.MEMBER);
        Authentication auth = new UsernamePasswordAuthenticationToken("member@demo.io", "password123");
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findByEmail("member@demo.io")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.name()).isEqualTo("Member");
    }

    @Test
    void login_badCredentials_propagates() {
        LoginRequest request = new LoginRequest("member@demo.io", "wrong");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
        verify(jwtService, never()).generateToken(any());
    }
}
