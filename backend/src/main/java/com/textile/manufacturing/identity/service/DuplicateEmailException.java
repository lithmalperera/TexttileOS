package com.textile.manufacturing.identity.service;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("A user with this email already exists: " + email);
    }
}
