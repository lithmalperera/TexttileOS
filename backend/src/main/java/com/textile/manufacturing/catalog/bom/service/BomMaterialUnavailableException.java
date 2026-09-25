package com.textile.manufacturing.catalog.bom.service;

public class BomMaterialUnavailableException extends RuntimeException {

    public BomMaterialUnavailableException(java.util.UUID materialId) {
        super("Material is unknown or archived: " + materialId);
    }
}
