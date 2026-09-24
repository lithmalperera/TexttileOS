package com.textile.manufacturing.catalog.material.service;

public class DuplicateMaterialCodeException extends RuntimeException {

    public DuplicateMaterialCodeException(String code) {
        super("A material with this code already exists: " + code);
    }
}
