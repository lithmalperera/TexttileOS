package com.textile.manufacturing.catalog.bom.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "bom")
public class Bom {

    @Id
    private UUID id;

    @Column(name = "product_id", nullable = false, unique = true)
    private UUID productId;

    @OneToMany(mappedBy = "bom", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<BomItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private java.time.Instant updatedAt;

    @Version
    private long version;

    protected Bom() {
    }

    private Bom(UUID productId) {
        this.id = UUID.randomUUID();
        this.productId = productId;
    }

    public static Bom createForProduct(UUID productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Product is required");
        }
        return new Bom(productId);
    }

    public void addItem(UUID materialId, java.math.BigDecimal quantityPerProductUnit) {
        if (materialId == null) {
            throw new IllegalArgumentException("Material is required");
        }
        if (quantityPerProductUnit == null || quantityPerProductUnit.signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        boolean duplicate = items.stream().anyMatch(item -> item.getMaterialId().equals(materialId));
        if (duplicate) {
            throw new IllegalArgumentException("The same material cannot appear twice in a BOM");
        }
        items.add(new BomItem(this, materialId, normalizeQuantity(quantityPerProductUnit)));
    }

    private java.math.BigDecimal normalizeQuantity(java.math.BigDecimal quantity) {
        return quantity.setScale(6, java.math.RoundingMode.UNNECESSARY);
    }

    public boolean hasMaterial(UUID materialId) {
        return items.stream().anyMatch(item -> item.getMaterialId().equals(materialId));
    }

    public void updateQuantityFor(UUID materialId, java.math.BigDecimal quantityPerProductUnit) {
        items.stream()
            .filter(item -> item.getMaterialId().equals(materialId))
            .findFirst()
            .ifPresentOrElse(
                item -> item.updateQuantity(quantityPerProductUnit),
                () -> {
                    throw new IllegalArgumentException("Material is not part of this BOM: " + materialId);
                });
    }

    public void retainMaterials(java.util.Set<UUID> materialIds) {
        items.removeIf(item -> !materialIds.contains(item.getMaterialId()));
    }

    public void clearItems() {
        items.clear();
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public List<BomItem> getItems() {
        return List.copyOf(items);
    }

    public java.time.Instant getUpdatedAt() {
        return updatedAt;
    }
}
