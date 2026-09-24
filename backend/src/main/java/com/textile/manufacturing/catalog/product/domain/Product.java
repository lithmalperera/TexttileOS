package com.textile.manufacturing.catalog.product.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "product")
public class Product {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String code;

    @Column(name = "code_normalized", nullable = false, unique = true)
    private String codeNormalized;

    @Column(nullable = false)
    private String name;

    @Column
    private String category;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "output_unit", nullable = false)
    private OutputUnit outputUnit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Version
    private long version;

    protected Product() {
    }

    private Product(UUID id, String code, String codeNormalized, String name, String category,
                    String description, OutputUnit outputUnit) {
        this.id = id;
        this.code = code;
        this.codeNormalized = codeNormalized;
        this.name = name;
        this.category = category;
        this.description = description;
        this.outputUnit = outputUnit;
        this.status = ProductStatus.ACTIVE;
    }

    public static Product register(
        String code, String name, String category, String description, OutputUnit outputUnit) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Product code is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (outputUnit == null) {
            throw new IllegalArgumentException("Output unit is required");
        }
        return new Product(
            UUID.randomUUID(),
            code.trim(),
            code.trim().toLowerCase(Locale.ROOT),
            name.trim(),
            category,
            description,
            outputUnit);
    }

    public void updateDetails(String name, String category, String description) {
        if (status != ProductStatus.ACTIVE) {
            throw new IllegalStateException("Archived products cannot be updated");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }
        this.name = name.trim();
        this.category = category;
        this.description = description;
    }

    public void archive() {
        if (status == ProductStatus.ARCHIVED) {
            throw new IllegalStateException("Product is already archived");
        }
        this.status = ProductStatus.ARCHIVED;
        this.archivedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public OutputUnit getOutputUnit() {
        return outputUnit;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }
}
