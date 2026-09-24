package com.mirae.elibrary.security;

import com.mirae.elibrary.config.AppProperties;
import com.mirae.elibrary.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

/**
 * Issues and validates HMAC-signed JWT access tokens. The token subject is the
 * user id; email and role are carried as claims for convenience.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(AppProperties properties) {
        byte[] secretBytes = Base64.getDecoder().decode(properties.getJwt().getSecret());
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expirationMs = properties.getJwt().getExpirationMs();
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * Parses and verifies the token, returning its claims. Throws a
     * {@link io.jsonwebtoken.JwtException} subtype if invalid or expired.
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
