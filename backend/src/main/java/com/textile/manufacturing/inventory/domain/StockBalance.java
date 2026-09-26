package com.textile.manufacturing.inventory.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "stock_balance")
public class StockBalance {

    @Id
    private UUID id;

    @Column(name = "inventory_item_id", nullable = false, unique = true)
    private UUID inventoryItemId;

    @Column(name = "on_hand_quantity", nullable = false)
    private BigDecimal onHandQuantity = BigDecimal.ZERO;

    @Column(name = "reserved_quantity", nullable = false)
    private BigDecimal reservedQuantity = BigDecimal.ZERO;

    @Version
    private long version;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StockBalance() {
    }

    private StockBalance(UUID inventoryItemId) {
        this.id = UUID.randomUUID();
        this.inventoryItemId = inventoryItemId;
        this.onHandQuantity = BigDecimal.ZERO.setScale(6);
        this.reservedQuantity = BigDecimal.ZERO.setScale(6);
    }

    public static StockBalance createEmpty(UUID inventoryItemId) {
        if (inventoryItemId == null) {
            throw new IllegalArgumentException("Inventory item is required");
        }
        return new StockBalance(inventoryItemId);
    }

    public void receive(BigDecimal amount) {
        requirePositive(amount, "Received quantity");
        onHandQuantity = onHandQuantity.add(normalize(amount));
    }

    public void adjustOnHandBy(BigDecimal onHandDelta) {
        BigDecimal normalized = normalize(onHandDelta);
        BigDecimal resulting = onHandQuantity.add(normalized);

        if (resulting.signum() < 0) {
            throw new IllegalArgumentException(
                "Adjustment would make on-hand stock negative (current: %s, delta: %s)"
                    .formatted(onHandQuantity.toPlainString(), normalized.toPlainString()));
        }
        onHandQuantity = resulting;
    }

    public void reserve(BigDecimal amount) {
        requirePositive(amount, "Reserved quantity");
        BigDecimal normalized = normalize(amount);

        if (availableQuantity().compareTo(normalized) < 0) {
            throw new IllegalArgumentException(
                "Insufficient available stock (available: %s, requested: %s)"
                    .formatted(availableQuantity().toPlainString(), normalized.toPlainString()));
        }
        reservedQuantity = reservedQuantity.add(normalized);
    }

    public void release(BigDecimal amount) {
        requirePositive(amount, "Released quantity");
        BigDecimal normalized = normalize(amount);

        if (reservedQuantity.compareTo(normalized) < 0) {
            throw new IllegalArgumentException(
                "Cannot release more than is reserved (reserved: %s, requested: %s)"
                    .formatted(reservedQuantity.toPlainString(), normalized.toPlainString()));
        }
        reservedQuantity = reservedQuantity.subtract(normalized);
    }

    public void consume(BigDecimal amount) {
        requirePositive(amount, "Consumed quantity");
        BigDecimal normalized = normalize(amount);

        if (reservedQuantity.compareTo(normalized) < 0) {
            throw new IllegalArgumentException(
                "Cannot consume more than is reserved (reserved: %s, requested: %s)"
                    .formatted(reservedQuantity.toPlainString(), normalized.toPlainString()));
        }
        if (onHandQuantity.compareTo(normalized) < 0) {
            throw new IllegalArgumentException(
                "Cannot consume more than is on hand (on hand: %s, requested: %s)"
                    .formatted(onHandQuantity.toPlainString(), normalized.toPlainString()));
        }
        onHandQuantity = onHandQuantity.subtract(normalized);
        reservedQuantity = reservedQuantity.subtract(normalized);
    }

    public BigDecimal availableQuantity() {
        return onHandQuantity.subtract(reservedQuantity);
    }

    public BigDecimal getOnHandQuantity() {
        return onHandQuantity;
    }

    public BigDecimal getReservedQuantity() {
        return reservedQuantity;
    }

    public UUID getInventoryItemId() {
        return inventoryItemId;
    }

    private void requirePositive(BigDecimal amount, String label) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(label + " must be greater than zero");
        }
    }

    private static BigDecimal normalize(BigDecimal quantity) {
        return quantity.setScale(6, RoundingMode.UNNECESSARY);
    }
}
