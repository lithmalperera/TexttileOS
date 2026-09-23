package com.textile.manufacturing.identity.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.textile.manufacturing.identity.domain.AppUser;
import com.textile.manufacturing.identity.domain.UserStatus;
import com.textile.manufacturing.identity.repository.AppUserRepository;

@Service
public class AuthenticationService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    AuthenticationService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public AppUser authenticate(String email, String rawPassword) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);

        AppUser user = appUserRepository.findByEmailNormalized(normalized)
            .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidCredentialsException();
        }

        return user;
    }
}
