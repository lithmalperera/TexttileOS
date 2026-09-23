package com.textile.manufacturing.identity.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.textile.manufacturing.identity.domain.AppUser;

public record UserResponse(
    UUID id,
    String email,
    String displayName,
    String status,
    List<String> roles,
    Instant createdAt) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getDisplayName(),
            user.getStatus().name(),
            user.getRoles().stream().map(role -> role.getCode()).sorted().toList(),
            user.getCreatedAt());
    }
}
