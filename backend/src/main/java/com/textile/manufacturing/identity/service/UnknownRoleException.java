package com.textile.manufacturing.identity.service;

import java.util.Set;

public class UnknownRoleException extends RuntimeException {

    public UnknownRoleException(Set<String> roleCodes) {
        super("Unknown roles: " + roleCodes);
    }
}
