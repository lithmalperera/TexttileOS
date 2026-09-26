package com.textile.manufacturing.inventory.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.textile.manufacturing.inventory.domain.InventoryMovement;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {

    @Query("""
        select m from InventoryMovement m
        join fetch m.transaction t
        where m.inventoryItemId = :inventoryItemId
        order by t.occurredAt desc, m.id desc
        """)
    List<InventoryMovement> findHistory(UUID inventoryItemId);
}
