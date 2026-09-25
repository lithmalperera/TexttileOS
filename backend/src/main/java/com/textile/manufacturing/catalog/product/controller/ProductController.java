package com.textile.manufacturing.catalog.product.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.textile.manufacturing.catalog.product.domain.Product;
import com.textile.manufacturing.catalog.product.domain.ProductStatus;
import com.textile.manufacturing.catalog.product.dto.CreateProductRequest;
import com.textile.manufacturing.catalog.product.dto.ProductResponse;
import com.textile.manufacturing.catalog.product.dto.UpdateProductRequest;
import com.textile.manufacturing.catalog.product.service.ProductService;
import com.textile.manufacturing.common.web.PageResponse;

import jakarta.validation.Valid;

import org.springdoc.core.annotations.ParameterObject;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    PageResponse<ProductResponse> listProducts(
        @RequestParam(required = false) ProductStatus status,
        @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.of(productService.listProducts(status, pageable), ProductResponse::from);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER')")
    ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        Product product = productService.createProduct(
            request.code(), request.name(), request.category(), request.description(),
            request.outputUnit());

        ProductResponse response = ProductResponse.from(product);
        return ResponseEntity
            .created(URI.create("/api/v1/products/" + response.id()))
            .body(response);
    }

    @GetMapping("/{productId}")
    ProductResponse getProduct(@PathVariable UUID productId) {
        return ProductResponse.from(productService.getProduct(productId));
    }

    @PatchMapping("/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER')")
    ProductResponse updateProduct(@PathVariable UUID productId, @Valid @RequestBody UpdateProductRequest request) {
        return ProductResponse.from(productService.updateProduct(
            productId, request.name(), request.category(), request.description()));
    }

    @PostMapping("/{productId}/archival")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER')")
    ProductResponse archiveProduct(@PathVariable UUID productId) {
        return ProductResponse.from(productService.archiveProduct(productId));
    }
}
