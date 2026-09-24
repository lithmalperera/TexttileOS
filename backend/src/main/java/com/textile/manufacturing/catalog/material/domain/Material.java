package com.textile.manufacturing.catalog.material.domain;

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
@Table(name = "material")
public class Material {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String code;

    @Column(name = "code_normalized", nullable = false, unique = true)
    private String codeNormalized;

    @Column(nullable = false)
    private String name;

    @Column(name = "material_type")
    private String materialType;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_unit", nullable = false)
    private BaseUnit baseUnit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaterialStatus status;

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

    protected Material() {
    }

    private Material(UUID id, String code, String codeNormalized, String name,
                     String materialType, BaseUnit baseUnit) {
        this.id = id;
        this.code = code;
        this.codeNormalized = codeNormalized;
        this.name = name;
        this.materialType = materialType;
        this.baseUnit = baseUnit;
        this.status = MaterialStatus.ACTIVE;
    }

    public static Material register(String code, String name, String materialType, BaseUnit baseUnit) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Material code is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Material name is required");
        }
        if (baseUnit == null) {
            throw new IllegalArgumentException("Base unit is required");
        }
        return new Material(
            UUID.randomUUID(),
            code.trim(),
            code.trim().toLowerCase(Locale.ROOT),
            name.trim(),
            materialType,
            baseUnit);
    }

    public void updateDetails(String name, String materialType) {
        if (status != MaterialStatus.ACTIVE) {
            throw new IllegalStateException("Archived materials cannot be updated");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Material name is required");
        }
        this.name = name.trim();
        this.materialType = materialType;
    }

    public void archive() {
        if (status == MaterialStatus.ARCHIVED) {
            throw new IllegalStateException("Material is already archived");
        }
        this.status = MaterialStatus.ARCHIVED;
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

    public String getMaterialType() {
        return materialType;
    }

    public BaseUnit getBaseUnit() {
        return baseUnit;
    }

    public MaterialStatus getStatus() {
        return status;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }
}
