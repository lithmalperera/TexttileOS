package com.textile.manufacturing.catalog.material.service;

import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.textile.manufacturing.catalog.material.domain.BaseUnit;
import com.textile.manufacturing.catalog.material.domain.Material;
import com.textile.manufacturing.catalog.material.domain.MaterialStatus;
import com.textile.manufacturing.catalog.material.repository.MaterialRepository;

@Service
public class MaterialService {

    private final MaterialRepository materialRepository;

    MaterialService(MaterialRepository materialRepository) {
        this.materialRepository = materialRepository;
    }

    @Transactional
    public Material createMaterial(String code, String name, String materialType, BaseUnit baseUnit) {
        String normalized = code.trim().toLowerCase(Locale.ROOT);

        if (materialRepository.existsByCodeNormalized(normalized)) {
            throw new DuplicateMaterialCodeException(code);
        }

        return materialRepository.save(Material.register(code, name, materialType, baseUnit));
    }

    @Transactional(readOnly = true)
    public Material getMaterial(UUID materialId) {
        return findMaterial(materialId);
    }

    @Transactional(readOnly = true)
    public Page<Material> listMaterials(MaterialStatus status, Pageable pageable) {
        if (status == null) {
            return materialRepository.findAll(pageable);
        }
        return materialRepository.findByStatus(status, pageable);
    }

    @Transactional
    public Material updateMaterial(UUID materialId, String name, String materialType) {
        Material material = findMaterial(materialId);
        material.updateDetails(name, materialType);
        return material;
    }

    @Transactional
    public Material archiveMaterial(UUID materialId) {
        Material material = findMaterial(materialId);
        material.archive();
        return material;
    }

    private Material findMaterial(UUID materialId) {
        return materialRepository.findById(materialId).orElseThrow(() -> new MaterialNotFoundException(materialId));
    }
}
