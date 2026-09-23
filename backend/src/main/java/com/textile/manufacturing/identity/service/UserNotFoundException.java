package com.textile.manufacturing.identity.service;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(java.util.UUID id) {
        super("User not found: " + id);
    }
}
