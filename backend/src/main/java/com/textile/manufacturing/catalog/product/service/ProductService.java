package com.textile.manufacturing.catalog.product.service;

import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.textile.manufacturing.catalog.product.domain.OutputUnit;
import com.textile.manufacturing.catalog.product.domain.Product;
import com.textile.manufacturing.catalog.product.domain.ProductStatus;
import com.textile.manufacturing.catalog.product.repository.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public Product createProduct(String code, String name, String category, String description, OutputUnit outputUnit) {
        String normalized = code.trim().toLowerCase(Locale.ROOT);

        if (productRepository.existsByCodeNormalized(normalized)) {
            throw new DuplicateProductCodeException(code);
        }

        return productRepository.save(Product.register(code, name, category, description, outputUnit));
    }

    @Transactional(readOnly = true)
    public Product getProduct(UUID productId) {
        return findProduct(productId);
    }

    @Transactional(readOnly = true)
    public Page<Product> listProducts(ProductStatus status, Pageable pageable) {
        if (status == null) {
            return productRepository.findAll(pageable);
        }
        return productRepository.findByStatus(status, pageable);
    }

    @Transactional
    public Product updateProduct(UUID productId, String name, String category, String description) {
        Product product = findProduct(productId);
        product.updateDetails(name, category, description);
        return product;
    }

    @Transactional
    public Product archiveProduct(UUID productId) {
        Product product = findProduct(productId);
        product.archive();
        return product;
    }

    private Product findProduct(UUID productId) {
        return productRepository.findById(productId).orElseThrow(() -> new ProductNotFoundException(productId));
    }
}
