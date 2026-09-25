package com.textile.manufacturing.catalog.material.controller;

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

import com.textile.manufacturing.catalog.material.domain.Material;
import com.textile.manufacturing.catalog.material.domain.MaterialStatus;
import com.textile.manufacturing.catalog.material.dto.CreateMaterialRequest;
import com.textile.manufacturing.catalog.material.dto.MaterialResponse;
import com.textile.manufacturing.catalog.material.dto.UpdateMaterialRequest;
import com.textile.manufacturing.catalog.material.service.MaterialService;
import com.textile.manufacturing.common.web.PageResponse;

import jakarta.validation.Valid;

import org.springdoc.core.annotations.ParameterObject;

@RestController
@RequestMapping("/api/v1/materials")
public class MaterialController {

    private final MaterialService materialService;

    MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping
    PageResponse<MaterialResponse> listMaterials(
        @RequestParam(required = false) MaterialStatus status,
        @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.of(materialService.listMaterials(status, pageable), MaterialResponse::from);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER')")
    ResponseEntity<MaterialResponse> createMaterial(@Valid @RequestBody CreateMaterialRequest request) {
        Material material = materialService.createMaterial(
            request.code(), request.name(), request.materialType(), request.baseUnit());

        MaterialResponse response = MaterialResponse.from(material);
        return ResponseEntity
            .created(URI.create("/api/v1/materials/" + response.id()))
            .body(response);
    }

    @GetMapping("/{materialId}")
    MaterialResponse getMaterial(@PathVariable UUID materialId) {
        return MaterialResponse.from(materialService.getMaterial(materialId));
    }

    @PatchMapping("/{materialId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER')")
    MaterialResponse updateMaterial(@PathVariable UUID materialId, @Valid @RequestBody UpdateMaterialRequest request) {
        return MaterialResponse.from(materialService.updateMaterial(materialId, request.name(), request.materialType()));
    }

    @PostMapping("/{materialId}/archival")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER')")
    MaterialResponse archiveMaterial(@PathVariable UUID materialId) {
        return MaterialResponse.from(materialService.archiveMaterial(materialId));
    }
}
