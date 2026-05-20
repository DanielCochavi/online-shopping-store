package com.example.onlineshopping.security;

import com.example.onlineshopping.model.UserRole;
import com.example.onlineshopping.model.User;
import com.example.onlineshopping.dto.AuthResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final Duration expiration;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration}") Duration expiration) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public AuthResponse generateToken(User user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(expiration);
        String token = Jwts.builder()
                .subject(user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();

        return new AuthResponse(token, "Bearer", user.getId(), user.getRole(), expiresAt);
    }

    public JwtPrincipal parseToken(String token) {
        // JWT verifies both the signature and expiration before exposing claims.
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String subject = claims.getSubject();
        String roleValue = claims.get("role", String.class);
        if (subject == null || subject.isBlank() || roleValue == null || roleValue.isBlank()) {
            throw new IllegalArgumentException("Token subject or role is missing");
        }
        return new JwtPrincipal(subject, UserRole.valueOf(roleValue));
    }
}
