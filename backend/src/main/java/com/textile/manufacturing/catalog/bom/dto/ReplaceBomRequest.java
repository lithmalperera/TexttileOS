package com.textile.manufacturing.catalog.bom.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Digits;

public record ReplaceBomRequest(
    @NotEmpty @Valid List<Item> items) {

    public record Item(
        @NotNull UUID materialId,
        @NotNull @Positive @Digits(integer = 13, fraction = 6) BigDecimal quantityPerProductUnit) {
    }
}
