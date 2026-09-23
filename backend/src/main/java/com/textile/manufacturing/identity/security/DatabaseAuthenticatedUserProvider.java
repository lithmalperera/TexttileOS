package com.textile.manufacturing.identity.security;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.textile.manufacturing.common.security.AuthenticatedUser;
import com.textile.manufacturing.common.security.AuthenticatedUserProvider;
import com.textile.manufacturing.identity.domain.UserStatus;
import com.textile.manufacturing.identity.repository.AppUserRepository;

@Component
public class DatabaseAuthenticatedUserProvider implements AuthenticatedUserProvider {

    private final AppUserRepository appUserRepository;

    DatabaseAuthenticatedUserProvider(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public AuthenticatedUser loadActiveUser(UUID userId) {
        return appUserRepository.findById(userId)
            .filter(user -> user.getStatus() == UserStatus.ACTIVE)
            .map(user -> new AuthenticatedUser(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRoles().stream().map(role -> role.getCode()).sorted().toList()))
            .orElse(null);
    }
}
