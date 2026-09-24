package com.mirae.elibrary.service;

import com.mirae.elibrary.config.AppProperties;
import com.mirae.elibrary.domain.Role;
import com.mirae.elibrary.domain.User;
import com.mirae.elibrary.exception.BusinessRuleException;
import com.mirae.elibrary.repository.UserRepository;
import com.mirae.elibrary.security.JwtService;
import com.mirae.elibrary.web.dto.AuthResponse;
import com.mirae.elibrary.web.dto.LoginRequest;
import com.mirae.elibrary.web.dto.RegisterRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration and login. Passwords are hashed with BCrypt; a successful call
 * returns a signed JWT the client uses as a bearer token.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BusinessRuleException("Email already registered: " + email);
        }
        User user = new User(
                email,
                passwordEncoder.encode(request.password()),
                request.name().trim(),
                Role.MEMBER);
        userRepository.save(user);
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        // Delegates credential checking to Spring Security's provider chain.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));
        // Authentication succeeded, so the user is guaranteed to exist.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpirationMs(),
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name());
    }
}
