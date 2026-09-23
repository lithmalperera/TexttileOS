package com.textile.manufacturing.identity.service;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.textile.manufacturing.identity.domain.AppUser;
import com.textile.manufacturing.identity.domain.Role;
import com.textile.manufacturing.identity.repository.AppUserRepository;
import com.textile.manufacturing.identity.repository.RoleRepository;

@Service
public class UserService {

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    UserService(AppUserRepository appUserRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AppUser createUser(String email, String displayName, String rawPassword, Set<String> roleCodes) {
        Set<String> requestedCodes = Set.copyOf(roleCodes);
        String normalized = email.trim().toLowerCase(Locale.ROOT);

        if (appUserRepository.existsByEmailNormalized(normalized)) {
            throw new DuplicateEmailException(email);
        }

        Set<Role> roles = new LinkedHashSet<>(roleRepository.findByCodeIn(requestedCodes));
        if (roles.size() != requestedCodes.size()) {
            throw new UnknownRoleException(requestedCodes);
        }

        AppUser user = AppUser.register(email, displayName, passwordEncoder.encode(rawPassword), roles);
        return appUserRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Page<AppUser> listUsers(Pageable pageable) {
        return appUserRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public AppUser getUser(UUID id) {
        return findUser(id);
    }

    @Transactional
    public AppUser deactivateUser(UUID id) {
        AppUser user = findUser(id);
        user.deactivate();
        return user;
    }

    private AppUser findUser(UUID id) {
        return appUserRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }
}
