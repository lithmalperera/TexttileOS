package com.textile.manufacturing.inventory.dto;

import java.util.List;
import java.util.UUID;

import com.textile.manufacturing.inventory.service.InventoryService;

public record StockMovementResponse(
    UUID transactionId,
    String transactionType,
    String reason,
    UUID inventoryItemId,
    String onHandDelta,
    String reservedDelta,
    String occurredAt) {

    public static StockMovementResponse from(InventoryService.StockMovementView view) {
        return new StockMovementResponse(
            view.transactionId(),
            view.transactionType(),
            view.reason(),
            view.inventoryItemId(),
            view.onHandDelta(),
            view.reservedDelta(),
            view.occurredAt().toString());
    }

    public static List<StockMovementResponse> from(List<InventoryService.StockMovementView> views) {
        return views.stream().map(StockMovementResponse::from).toList();
    }
}
