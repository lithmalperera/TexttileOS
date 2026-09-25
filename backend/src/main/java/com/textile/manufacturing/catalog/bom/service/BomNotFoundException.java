package com.textile.manufacturing.catalog.bom.service;

import java.util.UUID;

public class BomNotFoundException extends RuntimeException {

    public BomNotFoundException(UUID productId) {
        super("No BOM is defined for product: " + productId);
    }
}
