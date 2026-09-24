package com.textile.manufacturing.catalog.product.service;

public class DuplicateProductCodeException extends RuntimeException {

    public DuplicateProductCodeException(String code) {
        super("A product with this code already exists: " + code);
    }
}
