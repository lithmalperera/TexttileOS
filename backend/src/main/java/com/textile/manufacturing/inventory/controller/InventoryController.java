package com.textile.manufacturing.inventory.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.textile.manufacturing.common.security.AuthenticatedUser;
import com.textile.manufacturing.inventory.dto.AdjustmentRequest;
import com.textile.manufacturing.inventory.dto.ReceiptRequest;
import com.textile.manufacturing.inventory.dto.StockBalanceResponse;
import com.textile.manufacturing.inventory.dto.StockMovementResponse;
import com.textile.manufacturing.inventory.service.InventoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/balances")
    List<StockBalanceResponse> listBalances() {
        return inventoryService.listBalances().stream()
            .map(StockBalanceResponse::from)
            .toList();
    }

    @GetMapping("/balances/{inventoryItemId}")
    StockBalanceResponse getBalance(@PathVariable UUID inventoryItemId) {
        return StockBalanceResponse.from(inventoryService.getBalanceByItem(inventoryItemId));
    }

    @GetMapping("/movements")
    List<StockMovementResponse> listMovements(@RequestParam UUID materialId) {
        return StockMovementResponse.from(inventoryService.listMaterialMovements(materialId));
    }

    @PostMapping("/receipts")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER')")
    ResponseEntity<StockBalanceResponse> receiveStock(
        @Valid @RequestBody ReceiptRequest request,
        @AuthenticationPrincipal AuthenticatedUser user) {
        var view = inventoryService.receiveMaterial(
            request.materialId(), request.quantity(), request.reason(), user.id());
        return ResponseEntity.ok(StockBalanceResponse.from(view));
    }

    @PostMapping("/adjustments")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER')")
    ResponseEntity<StockBalanceResponse> adjustStock(
        @Valid @RequestBody AdjustmentRequest request,
        @AuthenticationPrincipal AuthenticatedUser user) {
        var view = inventoryService.adjustMaterialStock(
            request.materialId(), request.onHandDelta(), request.reason(), user.id());
        return ResponseEntity.ok(StockBalanceResponse.from(view));
    }
}
