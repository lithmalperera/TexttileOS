package com.textile.manufacturing.catalog.bom.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.textile.manufacturing.catalog.bom.dto.BomCalculationRequest;
import com.textile.manufacturing.catalog.bom.dto.BomCalculationResponse;
import com.textile.manufacturing.catalog.bom.dto.BomResponse;
import com.textile.manufacturing.catalog.bom.dto.ReplaceBomRequest;
import com.textile.manufacturing.catalog.bom.service.BomService;

import jakarta.validation.Valid;

@RestController
public class BomController {

    private final BomService bomService;

    BomController(BomService bomService) {
        this.bomService = bomService;
    }

    @GetMapping("/api/v1/products/{productId}/bom")
    BomResponse getBom(@PathVariable UUID productId) {
        return BomResponse.from(bomService.getBom(productId));
    }

    @PutMapping("/api/v1/products/{productId}/bom")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER')")
    BomResponse replaceBom(@PathVariable UUID productId, @Valid @RequestBody ReplaceBomRequest request) {
        var inputs = request.items().stream()
            .map(item -> new BomService.BomItemInput(item.materialId(), item.quantityPerProductUnit()))
            .toList();

        return BomResponse.from(bomService.replaceBom(productId, inputs));
    }

    @PostMapping("/api/v1/bom-calculations")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER')")
    ResponseEntity<BomCalculationResponse> calculateRequirements(@Valid @RequestBody BomCalculationRequest request) {
        return ResponseEntity.ok(
            BomCalculationResponse.from(bomService.calculateRequirements(request.productId(), request.quantity())));
    }
}
