package com.textile.manufacturing.catalog.bom.dto;

import java.util.List;
import java.util.UUID;

import com.textile.manufacturing.catalog.bom.service.BomService;

public record BomResponse(
    UUID bomId,
    UUID productId,
    List<BomItemResponse> items) {

    public record BomItemResponse(
        UUID materialId,
        String materialCode,
        String materialName,
        String unit,
        String quantityPerProductUnit) {

        public static BomItemResponse from(BomService.BomItemView view) {
            return new BomItemResponse(
                view.materialId(),
                view.materialCode(),
                view.materialName(),
                view.unit(),
                view.quantityPerProductUnit().toPlainString());
        }
    }

    public static BomResponse from(BomService.BomView view) {
        return new BomResponse(
            view.bomId(),
            view.productId(),
            view.items().stream().map(BomItemResponse::from).toList());
    }
}
