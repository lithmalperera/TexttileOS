package com.textile.manufacturing.inventory.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReceiptRequest(
    @NotNull UUID materialId,
    @NotNull @Positive @Digits(integer = 13, fraction = 6) BigDecimal quantity,
    @NotBlank @Size(max = 500) String reason) {
}
