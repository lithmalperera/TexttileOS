package com.textile.manufacturing.catalog.material.dto;

import java.time.Instant;
import java.util.UUID;

import com.textile.manufacturing.catalog.material.domain.Material;

public record MaterialResponse(
    UUID id,
    String code,
    String name,
    String materialType,
    String baseUnit,
    String status,
    Instant archivedAt) {

    public static MaterialResponse from(Material material) {
        return new MaterialResponse(
            material.getId(),
            material.getCode(),
            material.getName(),
            material.getMaterialType(),
            material.getBaseUnit().name(),
            material.getStatus().name(),
            material.getArchivedAt());
    }
}
