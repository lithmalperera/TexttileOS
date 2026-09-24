package com.textile.manufacturing.catalog.product.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.textile.manufacturing.catalog.product.domain.Product;
import com.textile.manufacturing.catalog.product.domain.ProductStatus;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    boolean existsByCodeNormalized(String codeNormalized);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);
}
