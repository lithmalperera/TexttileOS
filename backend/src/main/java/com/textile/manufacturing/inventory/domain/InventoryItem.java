package com.textile.manufacturing.inventory.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory_item")
public class InventoryItem {

    @Id
    private UUID id;

    @Column(name = "stock_location_id", nullable = false)
    private UUID stockLocationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_kind", nullable = false)
    private StockItemKind itemKind;

    @Column(name = "material_id")
    private UUID materialId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(nullable = false)
    private String unit;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected InventoryItem() {
    }

    private InventoryItem(UUID id, UUID stockLocationId, StockItemKind itemKind,
                          UUID materialId, UUID productId, String unit) {
        this.id = id;
        this.stockLocationId = stockLocationId;
        this.itemKind = itemKind;
        this.materialId = materialId;
        this.productId = productId;
        this.unit = unit;
    }

    public static InventoryItem forMaterial(UUID stockLocationId, UUID materialId, String unit) {
        if (stockLocationId == null || materialId == null || unit == null || unit.isBlank()) {
            throw new IllegalArgumentException("Location, material, and unit are required");
        }
        return new InventoryItem(
            UUID.randomUUID(), stockLocationId, StockItemKind.MATERIAL, materialId, null, unit.trim());
    }

    public static InventoryItem forProduct(UUID stockLocationId, UUID productId, String unit) {
        if (stockLocationId == null || productId == null || unit == null || unit.isBlank()) {
            throw new IllegalArgumentException("Location, product, and unit are required");
        }
        return new InventoryItem(
            UUID.randomUUID(), stockLocationId, StockItemKind.PRODUCT, null, productId, unit.trim());
    }

    public boolean isMaterialOf(UUID materialId) {
        return itemKind == StockItemKind.MATERIAL && materialId.equals(this.materialId);
    }

    public boolean isProductOf(UUID productId) {
        return itemKind == StockItemKind.PRODUCT && productId.equals(this.productId);
    }

    public UUID getId() {
        return id;
    }

    public UUID getStockLocationId() {
        return stockLocationId;
    }

    public StockItemKind getItemKind() {
        return itemKind;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getUnit() {
        return unit;
    }
}
