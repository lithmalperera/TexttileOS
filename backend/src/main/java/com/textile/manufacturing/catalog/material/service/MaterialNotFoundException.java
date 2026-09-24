package com.textile.manufacturing.catalog.material.service;

import java.util.UUID;

public class MaterialNotFoundException extends RuntimeException {

    public MaterialNotFoundException(java.util.UUID id) {
        super("Material not found: " + id);
    }
}
