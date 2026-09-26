package com.textile.manufacturing.catalog.material.service;

import java.util.UUID;

public class MaterialNotActiveException extends RuntimeException {

    public MaterialNotActiveException(UUID materialId) {
        super("Material is archived and cannot be used: " + materialId);
    }
}
