package com.textile.manufacturing.identity.dto;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(max = 100) String displayName,
    @NotBlank @Size(min = 12, max = 100) String password,
    @NotEmpty Set<String> roles) {
}
