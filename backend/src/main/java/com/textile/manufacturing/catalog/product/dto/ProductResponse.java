package com.textile.manufacturing.catalog.product.dto;

import java.time.Instant;
import java.util.UUID;

import com.textile.manufacturing.catalog.product.domain.Product;

public record ProductResponse(
    UUID id,
    String code,
    String name,
    String category,
    String description,
    String outputUnit,
    String status,
    Instant archivedAt) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getCode(),
            product.getName(),
            product.getCategory(),
            product.getDescription(),
            product.getOutputUnit().name(),
            product.getStatus().name(),
            product.getArchivedAt());
    }
}
