package com.textile.manufacturing.inventory.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory_movement")
public class InventoryMovement {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_transaction_id")
    private InventoryTransaction transaction;

    @Column(name = "inventory_item_id", nullable = false)
    private UUID inventoryItemId;

    @Column(name = "on_hand_delta", nullable = false)
    private BigDecimal onHandDelta;

    @Column(name = "reserved_delta", nullable = false)
    private BigDecimal reservedDelta;

    protected InventoryMovement() {
    }

    InventoryMovement(InventoryTransaction transaction, UUID inventoryItemId,
                      BigDecimal onHandDelta, BigDecimal reservedDelta) {
        this.id = UUID.randomUUID();
        this.transaction = transaction;
        this.inventoryItemId = inventoryItemId;
        this.onHandDelta = onHandDelta;
        this.reservedDelta = reservedDelta;
    }

    public UUID getId() {
        return id;
    }

    public InventoryTransaction getTransaction() {
        return transaction;
    }

    public UUID getInventoryItemId() {
        return inventoryItemId;
    }

    public BigDecimal getOnHandDelta() {
        return onHandDelta;
    }

    public BigDecimal getReservedDelta() {
        return reservedDelta;
    }
}
