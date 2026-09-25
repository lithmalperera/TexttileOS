package com.textile.manufacturing.catalog.bom.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BomCalculationRequest(
    @NotNull UUID productId,
    @NotNull @Positive Integer quantity) {
}
