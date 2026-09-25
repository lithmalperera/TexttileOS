package com.textile.manufacturing.catalog.bom.domain;

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
@Table(name = "bom_item")
public class BomItem {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bom_id")
    private Bom bom;

    @Column(name = "material_id", nullable = false)
    private UUID materialId;

    @Column(name = "quantity_per_product_unit", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantityPerProductUnit;

    protected BomItem() {
    }

    BomItem(Bom bom, UUID materialId, BigDecimal quantityPerProductUnit) {
        this.id = UUID.randomUUID();
        this.bom = bom;
        this.materialId = materialId;
        this.quantityPerProductUnit = quantityPerProductUnit;
    }

    void updateQuantity(BigDecimal newQuantity) {
        if (newQuantity == null || newQuantity.signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        this.quantityPerProductUnit = newQuantity.setScale(6, java.math.RoundingMode.UNNECESSARY);
    }

    public UUID getId() {
        return id;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public BigDecimal getQuantityPerProductUnit() {
        return quantityPerProductUnit;
    }

    public BigDecimal requiredQuantityFor(int productQuantity) {
        return quantityPerProductUnit.multiply(BigDecimal.valueOf(productQuantity));
    }
}
