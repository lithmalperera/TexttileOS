package com.textile.manufacturing.catalog.material.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.textile.manufacturing.catalog.material.domain.Material;
import com.textile.manufacturing.catalog.material.domain.MaterialStatus;

public interface MaterialRepository extends JpaRepository<Material, UUID> {

    boolean existsByCodeNormalized(String codeNormalized);

    Optional<Material> findByCodeNormalized(String codeNormalized);

    Page<Material> findByStatus(MaterialStatus status, Pageable pageable);
}
