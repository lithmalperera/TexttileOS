package com.textile.manufacturing.catalog.product.dto;

import com.textile.manufacturing.catalog.product.domain.OutputUnit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProductRequest(
    @NotBlank @Size(max = 50) String code,
    @NotBlank @Size(max = 100) String name,
    @Size(max = 50) String category,
    @Size(max = 500) String description,
    @NotNull OutputUnit outputUnit) {
}
