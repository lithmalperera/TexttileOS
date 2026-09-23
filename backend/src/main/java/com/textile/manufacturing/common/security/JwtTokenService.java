package com.textile.manufacturing.common.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenService {

    public record TokenPayload(UUID userId, List<String> roles, Instant expiresAt) {
    }

    private final SecretKey signingKey;
    private final String issuer;
    private final long expiryMinutes;

    JwtTokenService(JwtProperties properties) {
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes());
        this.issuer = properties.issuer();
        this.expiryMinutes = properties.expiryMinutes();
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }

    public IssuedToken issueToken(UUID userId, List<String> roles) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expiryMinutes * 60);

        String token = Jwts.builder()
            .subject(userId.toString())
            .claim("roles", roles)
            .issuer(issuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(signingKey)
            .compact();

        return new IssuedToken(token, expiresAt);
    }

    @SuppressWarnings("unchecked")
    public TokenPayload parse(String token) {
        var claims = Jwts.parser()
            .verifyWith(signingKey)
            .requireIssuer(issuer)
            .build()
            .parseSignedClaims(token)
            .getPayload();

        List<String> roles = claims.get("roles", List.class);
        return new TokenPayload(
            UUID.fromString(claims.getSubject()),
            roles == null ? List.of() : roles,
            claims.getExpiration().toInstant());
    }
}
