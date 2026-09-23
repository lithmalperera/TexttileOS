package com.textile.manufacturing.identity.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.textile.manufacturing.common.security.JwtTokenService;
import com.textile.manufacturing.identity.domain.AppUser;
import com.textile.manufacturing.identity.dto.LoginRequest;
import com.textile.manufacturing.identity.dto.LoginResponse;
import com.textile.manufacturing.identity.dto.UserResponse;
import com.textile.manufacturing.identity.service.AuthenticationService;
import com.textile.manufacturing.identity.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserService userService;
    private final JwtTokenService jwtTokenService;

    AuthController(
        AuthenticationService authenticationService,
        UserService userService,
        JwtTokenService jwtTokenService) {
        this.authenticationService = authenticationService;
        this.userService = userService;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AppUser user = authenticationService.authenticate(request.email(), request.password());

        List<String> roleCodes = user.getRoles().stream().map(role -> role.getCode()).sorted().toList();
        JwtTokenService.IssuedToken issued = jwtTokenService.issueToken(user.getId(), roleCodes);

        LoginResponse response = new LoginResponse(
            issued.value(),
            "Bearer",
            issued.expiresAt(),
            UserResponse.from(user));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public UserResponse currentUser(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return UserResponse.from(userService.getUser(userId));
    }
}
