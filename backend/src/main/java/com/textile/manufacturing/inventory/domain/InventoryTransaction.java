package com.textile.manufacturing.inventory.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory_transaction")
public class InventoryTransaction {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private InventoryTransactionType transactionType;

    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(nullable = false)
    private String reason;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @CreationTimestamp
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private List<InventoryMovement> movements = new ArrayList<>();

    protected InventoryTransaction() {
    }

    private InventoryTransaction(InventoryTransactionType type, String reason,
                                 UUID actorUserId, String referenceType, UUID referenceId) {
        this.id = UUID.randomUUID();
        this.transactionType = type;
        this.reason = reason;
        this.actorUserId = actorUserId;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
    }

    public static InventoryTransaction create(
        InventoryTransactionType type, UUID actorUserId, String reason,
        String referenceType, UUID referenceId) {
        if (type == null) {
            throw new IllegalArgumentException("Transaction type is required");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required for every inventory transaction");
        }
        return new InventoryTransaction(type, reason.trim(), actorUserId, referenceType, referenceId);
    }

    public void addMovement(UUID inventoryItemId, BigDecimal onHandDelta, BigDecimal reservedDelta) {
        if (inventoryItemId == null) {
            throw new IllegalArgumentException("Inventory item is required");
        }
        movements.add(new InventoryMovement(
            this, inventoryItemId,
            normalize(onHandDelta), normalize(reservedDelta)));
    }

    private static BigDecimal normalize(BigDecimal delta) {
        if (delta == null) {
            throw new IllegalArgumentException("Movement delta is required");
        }
        return delta.setScale(6, java.math.RoundingMode.UNNECESSARY);
    }

    public UUID getId() {
        return id;
    }

    public InventoryTransactionType getTransactionType() {
        return transactionType;
    }

    public String getReason() {
        return reason;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public List<InventoryMovement> getMovements() {
        return List.copyOf(movements);
    }
}
