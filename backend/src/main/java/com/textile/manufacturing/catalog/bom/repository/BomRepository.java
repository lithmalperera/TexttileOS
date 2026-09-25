package com.textile.manufacturing.catalog.bom.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.textile.manufacturing.catalog.bom.domain.Bom;

public interface BomRepository extends JpaRepository<Bom, UUID> {

    Optional<Bom> findByProductId(UUID productId);

    boolean existsByProductId(UUID productId);
}
