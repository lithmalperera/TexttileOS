package com.textile.manufacturing.inventory.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.textile.manufacturing.inventory.domain.StockLocation;

public interface StockLocationRepository extends JpaRepository<StockLocation, UUID> {

    Optional<StockLocation> findByCode(String code);
}
