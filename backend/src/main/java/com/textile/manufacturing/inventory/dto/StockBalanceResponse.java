package com.textile.manufacturing.inventory.dto;

import java.util.UUID;

import com.textile.manufacturing.inventory.service.InventoryService;

public record StockBalanceResponse(
    UUID inventoryItemId,
    UUID materialId,
    String materialCode,
    String materialName,
    String unit,
    String onHandQuantity,
    String reservedQuantity,
    String availableQuantity) {

    public static StockBalanceResponse from(InventoryService.StockBalanceView view) {
        return new StockBalanceResponse(
            view.inventoryItemId(),
            view.materialId(),
            view.materialCode(),
            view.materialName(),
            view.unit(),
            view.onHandQuantity().toPlainString(),
            view.reservedQuantity().toPlainString(),
            view.availableQuantity().toPlainString());
    }
}
