package com.textile.manufacturing.inventory.service;

import java.util.UUID;

public class InventoryItemNotFoundException extends RuntimeException {

    public InventoryItemNotFoundException(UUID inventoryItemId) {
        super("Inventory item not found: " + inventoryItemId);
    }
}
