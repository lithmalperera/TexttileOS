package com.textile.manufacturing.identity.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.textile.manufacturing.common.web.PageResponse;
import com.textile.manufacturing.identity.domain.AppUser;
import com.textile.manufacturing.identity.dto.CreateUserRequest;
import com.textile.manufacturing.identity.dto.UserResponse;
import com.textile.manufacturing.identity.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    PageResponse<UserResponse> listUsers(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.of(userService.listUsers(pageable), UserResponse::from);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        AppUser user = userService.createUser(
            request.email(), request.displayName(), request.password(), request.roles());
        UserResponse response = UserResponse.from(user);
        return ResponseEntity
            .created(URI.create("/api/v1/users/" + response.id()))
            .body(response);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    UserResponse getUser(@PathVariable UUID userId) {
        return UserResponse.from(userService.getUser(userId));
    }

    @PostMapping("/{userId}/deactivation")
    @PreAuthorize("hasRole('ADMIN')")
    UserResponse deactivateUser(@PathVariable UUID userId) {
        return UserResponse.from(userService.deactivateUser(userId));
    }
}
