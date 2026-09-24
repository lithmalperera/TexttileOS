package com.textile.manufacturing.catalog.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProductRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 50) String category,
    @Size(max = 500) String description) {
}
