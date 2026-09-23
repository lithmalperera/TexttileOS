package com.textile.manufacturing.common.security;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.jsonwebtoken.ExpiredJwtException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTests {

    private final JwtProperties properties = new JwtProperties(
        "unit-test-secret-that-is-long-enough-for-hmac-0123456789", "test-issuer", 15);

    @Test
    void issuedTokenParsesBackToSubjectAndRoles() {
        UUID userId = UUID.randomUUID();

        var issued = service().issueToken(userId, List.of("PLANNER"));

        var payload = service().parse(issued.value());
        assertThat(payload.userId()).isEqualTo(userId);
        assertThat(payload.roles()).containsExactly("PLANNER");
        assertThat(payload.expiresAt())
            .isEqualTo(issued.expiresAt().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    void tokenWithZeroExpiryIsImmediatelyExpired() {
        var expiredService = new JwtTokenService(new JwtProperties(
            "unit-test-secret-that-is-long-enough-for-hmac-0123456789", "test-issuer", 0));

        var issued = expiredService.issueToken(UUID.randomUUID(), List.of("PLANNER"));

        assertThatThrownBy(() -> expiredService.parse(issued.value()))
            .isInstanceOf(ExpiredJwtException.class);
    }

    private JwtTokenService service() {
        return new JwtTokenService(properties);
    }
}
