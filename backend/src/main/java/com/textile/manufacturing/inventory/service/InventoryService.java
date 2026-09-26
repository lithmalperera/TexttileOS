package com.textile.manufacturing.inventory.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.textile.manufacturing.catalog.material.service.MaterialService;
import com.textile.manufacturing.inventory.domain.InventoryItem;
import com.textile.manufacturing.inventory.domain.InventoryTransaction;
import com.textile.manufacturing.inventory.domain.InventoryTransactionType;
import com.textile.manufacturing.inventory.domain.StockBalance;
import com.textile.manufacturing.inventory.repository.InventoryItemRepository;
import com.textile.manufacturing.inventory.repository.InventoryTransactionRepository;
import com.textile.manufacturing.inventory.repository.StockBalanceRepository;
import com.textile.manufacturing.inventory.repository.StockLocationRepository;

@Service
public class InventoryService {

    public record StockBalanceView(
        UUID inventoryItemId,
        UUID materialId,
        String materialCode,
        String materialName,
        String unit,
        BigDecimal onHandQuantity,
        BigDecimal reservedQuantity,
        BigDecimal availableQuantity) {
    }

    public record StockMovementView(
        UUID transactionId,
        String transactionType,
        String reason,
        UUID inventoryItemId,
        String onHandDelta,
        String reservedDelta,
        java.time.Instant occurredAt) {
    }

    private static final String MAIN_LOCATION_CODE = "MAIN";

    private final StockLocationRepository stockLocationRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final com.textile.manufacturing.inventory.repository.InventoryMovementRepository inventoryMovementRepository;
    private final MaterialService materialService;

    InventoryService(
        StockLocationRepository stockLocationRepository,
        InventoryItemRepository inventoryItemRepository,
        StockBalanceRepository stockBalanceRepository,
        InventoryTransactionRepository inventoryTransactionRepository,
        com.textile.manufacturing.inventory.repository.InventoryMovementRepository inventoryMovementRepository,
        MaterialService materialService) {
        this.stockLocationRepository = stockLocationRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.materialService = materialService;
    }

    @Transactional
    public StockBalanceView receiveMaterial(UUID materialId, BigDecimal quantity, String reason, UUID actorUserId) {
        var material = materialService.requireActiveMaterial(materialId);
        var item = resolveMaterialItem(materialId, material);
        var balance = getOrCreateBalance(item.getId());

        balance.receive(quantity);

        recordMovement(
            InventoryTransactionType.RECEIPT, actorUserId, reason,
            "MATERIAL", materialId,
            item.getId(), quantity, BigDecimal.ZERO);

        return toBalanceView(item, balance);
    }

    @Transactional
    public StockBalanceView adjustMaterialStock(UUID materialId, BigDecimal onHandDelta, String reason, UUID actorUserId) {
        var material = materialService.requireActiveMaterial(materialId);
        var item = resolveMaterialItem(materialId, material);
        var balance = getOrCreateBalance(item.getId());

        balance.adjustOnHandBy(onHandDelta);

        recordMovement(
            InventoryTransactionType.ADJUSTMENT, actorUserId, reason,
            "MATERIAL", materialId,
            item.getId(), onHandDelta, BigDecimal.ZERO);

        return toBalanceView(item, balance);
    }

    @Transactional(readOnly = true)
    public StockBalanceView getMaterialBalance(UUID materialId) {
        var material = materialService.getMaterialSummary(materialId);
        return inventoryItemRepository.findByStockLocationIdAndMaterialId(mainLocationId(), materialId)
            .map(item -> {
                var balance = stockBalanceRepository.findByInventoryItemId(item.getId())
                    .orElseGet(() -> StockBalance.createEmpty(item.getId()));
                return toBalanceView(item, balance);
            })
            .orElseGet(() -> new StockBalanceView(
                null, material.id(), material.code(), material.name(), material.baseUnit(),
                BigDecimal.ZERO.setScale(6), BigDecimal.ZERO.setScale(6), BigDecimal.ZERO.setScale(6)));
    }

    @Transactional(readOnly = true)
    public java.util.List<StockBalanceView> listBalances() {
        var itemsById = inventoryItemRepository.findAll().stream()
            .collect(java.util.stream.Collectors.toMap(
                InventoryItem::getId, item -> item));

        return stockBalanceRepository.findAll().stream()
            .map(balance -> {
                var item = itemsById.get(balance.getInventoryItemId());
                return toBalanceView(item, balance);
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public StockBalanceView getBalanceByItem(UUID inventoryItemId) {
        var item = inventoryItemRepository.findById(inventoryItemId)
            .orElseThrow(() -> new InventoryItemNotFoundException(inventoryItemId));
        var balance = stockBalanceRepository.findByInventoryItemId(inventoryItemId)
            .orElseGet(() -> StockBalance.createEmpty(inventoryItemId));
        return toBalanceView(item, balance);
    }

    @Transactional(readOnly = true)
    public java.util.List<StockMovementView> listMaterialMovements(UUID materialId) {
        return inventoryItemRepository.findByStockLocationIdAndMaterialId(mainLocationId(), materialId)
            .map(item -> inventoryMovementRepository.findHistory(item.getId()).stream()
                .map(movement -> {
                    var transaction = movement.getTransaction();
                    return new StockMovementView(
                        transaction.getId(),
                        transaction.getTransactionType().name(),
                        transaction.getReason(),
                        movement.getInventoryItemId(),
                        movement.getOnHandDelta().toPlainString(),
                        movement.getReservedDelta().toPlainString(),
                        transaction.getOccurredAt());
                })
                .toList())
            .orElseGet(java.util.List::of);
    }

    private InventoryItem resolveMaterialItem(UUID materialId, MaterialService.MaterialSummary material) {
        return inventoryItemRepository
            .findByStockLocationIdAndMaterialId(mainLocationId(), materialId)
            .orElseGet(() -> {
                var item = InventoryItem.forMaterial(
                    mainLocationId(), materialId, material.baseUnit());
                return inventoryItemRepository.save(item);
            });
    }

    private StockBalance getOrCreateBalance(UUID inventoryItemId) {
        return stockBalanceRepository.findByInventoryItemId(inventoryItemId)
            .orElseGet(() -> stockBalanceRepository.save(StockBalance.createEmpty(inventoryItemId)));
    }

    private void recordMovement(
        InventoryTransactionType type, UUID actorUserId, String reason,
        String referenceType, UUID referenceId,
        UUID inventoryItemId, BigDecimal onHandDelta, BigDecimal reservedDelta) {
        var transaction = InventoryTransaction.create(type, actorUserId, reason, referenceType, referenceId);
        transaction.addMovement(inventoryItemId, onHandDelta, reservedDelta);
        inventoryTransactionRepository.save(transaction);
    }

    private UUID mainLocationId() {
        return stockLocationRepository.findByCode(MAIN_LOCATION_CODE)
            .orElseThrow(() -> new IllegalStateException("The MAIN stock location is not seeded"))
            .getId();
    }

    private StockBalanceView toBalanceView(InventoryItem item, StockBalance balance) {
        var material = inventoryItemMaterial(item);
        return new StockBalanceView(
            item.getId(),
            item.getMaterialId(),
            material.code(),
            material.name(),
            item.getUnit(),
            balance.getOnHandQuantity(),
            balance.getReservedQuantity(),
            balance.availableQuantity());
    }

    private MaterialService.MaterialSummary inventoryItemMaterial(InventoryItem item) {
        if (item.getMaterialId() == null) {
            throw new IllegalStateException("Inventory item has no material reference");
        }
        return materialService.getMaterialSummary(item.getMaterialId());
    }
}
