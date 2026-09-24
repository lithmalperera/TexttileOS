package com.textile.manufacturing.catalog.material.dto;

import com.textile.manufacturing.catalog.material.domain.BaseUnit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMaterialRequest(
    @NotBlank @Size(max = 50) String code,
    @NotBlank @Size(max = 100) String name,
    @Size(max = 50) String materialType,
    @NotNull BaseUnit baseUnit) {
}
