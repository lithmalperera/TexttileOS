package com.textile.manufacturing.inventory.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.textile.manufacturing.inventory.domain.StockBalance;

public interface StockBalanceRepository extends JpaRepository<StockBalance, UUID> {

    Optional<StockBalance> findByInventoryItemId(UUID inventoryItemId);
}
