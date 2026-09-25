package com.textile.manufacturing.catalog.bom.dto;

import java.util.List;
import java.util.UUID;

import com.textile.manufacturing.catalog.bom.service.BomService;

public record BomCalculationResponse(List<CalculationItem> items) {

    public record CalculationItem(
        UUID materialId,
        String materialCode,
        String materialName,
        String unit,
        String quantityPerProductUnit,
        String requiredQuantity) {

        public static CalculationItem from(BomService.RequirementView view) {
            return new CalculationItem(
                view.materialId(),
                view.materialCode(),
                view.materialName(),
                view.unit(),
                view.quantityPerProductUnit().toPlainString(),
                view.requiredQuantity().toPlainString());
        }
    }

    public static BomCalculationResponse from(List<BomService.RequirementView> views) {
        return new BomCalculationResponse(views.stream().map(CalculationItem::from).toList());
    }
}
