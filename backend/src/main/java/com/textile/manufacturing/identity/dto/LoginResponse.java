package com.textile.manufacturing.identity.dto;

import java.time.Instant;

public record LoginResponse(
    String accessToken,
    String tokenType,
    Instant expiresAt,
    UserResponse user) {
}
