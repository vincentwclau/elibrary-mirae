package com.mirae.elibrary.web.dto;

import java.util.UUID;

/**
 * Returned by register/login. Contains the bearer token plus lightweight user
 * details so the frontend can render the session without an extra call.
 */
public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMs,
        UUID userId,
        String email,
        String name,
        String role
) {
}
