package com.textile.manufacturing.inventory.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.textile.manufacturing.inventory.domain.InventoryItem;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    Optional<InventoryItem> findByStockLocationIdAndMaterialId(UUID stockLocationId, UUID materialId);

    Optional<InventoryItem> findByStockLocationIdAndProductId(UUID stockLocationId, UUID productId);
}
