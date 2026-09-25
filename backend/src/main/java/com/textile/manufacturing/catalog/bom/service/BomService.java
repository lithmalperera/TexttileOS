package com.textile.manufacturing.catalog.bom.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.textile.manufacturing.catalog.bom.domain.Bom;
import com.textile.manufacturing.catalog.bom.repository.BomRepository;
import com.textile.manufacturing.catalog.material.domain.MaterialStatus;
import com.textile.manufacturing.catalog.material.repository.MaterialRepository;
import com.textile.manufacturing.catalog.product.service.ProductNotFoundException;
import com.textile.manufacturing.catalog.product.service.ProductService;

@Service
public class BomService {

    public record BomItemView(
        UUID materialId,
        String materialCode,
        String materialName,
        String unit,
        BigDecimal quantityPerProductUnit) {
    }

    public record BomView(UUID bomId, UUID productId, List<BomItemView> items) {
    }

    public record RequirementView(
        UUID materialId,
        String materialCode,
        String materialName,
        String unit,
        BigDecimal quantityPerProductUnit,
        BigDecimal requiredQuantity) {
    }

    public record BomItemInput(UUID materialId, BigDecimal quantityPerProductUnit) {
    }

    private final BomRepository bomRepository;
    private final MaterialRepository materialRepository;
    private final ProductService productService;

    BomService(BomRepository bomRepository, MaterialRepository materialRepository, ProductService productService) {
        this.bomRepository = bomRepository;
        this.materialRepository = materialRepository;
        this.productService = productService;
    }

    @Transactional(readOnly = true)
    public BomView getBom(UUID productId) {
        Bom bom = findBom(productId);
        return toView(bom);
    }

    @Transactional
    public BomView replaceBom(UUID productId, List<BomItemInput> itemInputs) {
        if (itemInputs == null || itemInputs.isEmpty()) {
            throw new IllegalArgumentException("A BOM must contain at least one material");
        }

        var product = productService.getProduct(productId);
        if (product.getStatus() != com.textile.manufacturing.catalog.product.domain.ProductStatus.ACTIVE) {
            throw new BomMaterialUnavailableException(product.getId());
        }

        java.util.Set<UUID> requestedMaterialIds = new java.util.LinkedHashSet<>();
        for (BomItemInput input : itemInputs) {
            if (!requestedMaterialIds.add(input.materialId())) {
                throw new IllegalArgumentException("The same material appears twice in the request: " + input.materialId());
            }
        }

        for (BomItemInput input : itemInputs) {
            materialRepository.findById(input.materialId())
                .filter(candidate -> candidate.getStatus() == MaterialStatus.ACTIVE)
                .orElseThrow(() -> new BomMaterialUnavailableException(input.materialId()));
        }

        Bom bom = bomRepository.findByProductId(productId).orElseGet(() -> Bom.createForProduct(productId));

        bom.retainMaterials(requestedMaterialIds);
        for (BomItemInput input : itemInputs) {
            if (bom.hasMaterial(input.materialId())) {
                bom.updateQuantityFor(input.materialId(), input.quantityPerProductUnit());
            } else {
                bom.addItem(input.materialId(), input.quantityPerProductUnit());
            }
        }

        return toView(bomRepository.save(bom));
    }

    @Transactional(readOnly = true)
    public List<RequirementView> calculateRequirements(UUID productId, int quantity) {
        Bom bom = findBom(productId);

        return bom.getItems().stream()
            .map(item -> {
                var material = materialRepository.findById(item.getMaterialId()).orElseThrow();
                var required = item.requiredQuantityFor(quantity);
                return new RequirementView(
                    material.getId(),
                    material.getCode(),
                    material.getName(),
                    material.getBaseUnit().name(),
                    item.getQuantityPerProductUnit(),
                    required);
            })
            .toList();
    }

    private Bom findBom(UUID productId) {
        return bomRepository.findByProductId(productId).orElseThrow(() -> new BomNotFoundException(productId));
    }

    private BomView toView(Bom bom) {
        List<BomItemView> items = bom.getItems().stream()
            .map(item -> {
                var material = materialRepository.findById(item.getMaterialId()).orElseThrow();
                return new BomItemView(
                    material.getId(),
                    material.getCode(),
                    material.getName(),
                    material.getBaseUnit().name(),
                    item.getQuantityPerProductUnit());
            })
            .toList();

        return new BomView(bom.getId(), bom.getProductId(), items);
    }
}
