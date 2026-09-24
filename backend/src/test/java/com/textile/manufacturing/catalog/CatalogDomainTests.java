package com.textile.manufacturing.catalog;

import org.junit.jupiter.api.Test;

import com.textile.manufacturing.catalog.material.domain.BaseUnit;
import com.textile.manufacturing.catalog.material.domain.Material;
import com.textile.manufacturing.catalog.product.domain.OutputUnit;
import com.textile.manufacturing.catalog.product.domain.Product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogDomainTests {

    @Test
    void productRegisterNormalizesCodeAndStartsActive() {
        Product product = Product.register(
            "  TSHIRT-BASIC ", "Basic T-Shirt", "Garments", "Cotton t-shirt", OutputUnit.PIECE);

        assertThat(product.getCode()).isEqualTo("TSHIRT-BASIC");
        assertThat(product.getOutputUnit()).isEqualTo(OutputUnit.PIECE);
        assertThat(product.getStatus().name()).isEqualTo("ACTIVE");
        assertThat(product.getArchivedAt()).isNull();
    }

    @Test
    void productArchiveIsOneWayAndBlocksUpdates() {
        Product product = Product.register("TSHIRT-X", "Test Shirt", null, null, OutputUnit.PIECE);

        product.archive();

        assertThat(product.getStatus().name()).isEqualTo("ARCHIVED");
        assertThat(product.getArchivedAt()).isNotNull();

        assertThatThrownBy(product::archive)
            .isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> product.updateDetails("New Name", null, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Archived");
    }

    @Test
    void productRegisterRejectsInvalidValues() {
        assertThatThrownBy(() -> Product.register(null, "Name", null, null, OutputUnit.PIECE))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Product.register("CODE", "  ", null, null, OutputUnit.PIECE))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Product.register("CODE", "Name", null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void materialArchiveIsOneWay() {
        Material material = Material.register("FABRIC-COTTON", "Cotton Fabric", "Fabric", BaseUnit.METER);

        material.archive();

        assertThat(material.getStatus().name()).isEqualTo("ARCHIVED");
        assertThatThrownBy(material::archive)
            .isInstanceOf(IllegalStateException.class);
    }
}
