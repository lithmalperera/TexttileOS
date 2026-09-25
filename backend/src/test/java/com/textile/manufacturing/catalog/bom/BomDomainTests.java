package com.textile.manufacturing.catalog.bom;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.textile.manufacturing.catalog.bom.domain.Bom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BomDomainTests {

    @Test
    void addItemValidatesAndRejectsDuplicates() {
        Bom bom = Bom.createForProduct(UUID.randomUUID());
        UUID materialId = UUID.randomUUID();

        bom.addItem(materialId, new BigDecimal("1.5"));

        assertThatThrownBy(() -> bom.addItem(materialId, new BigDecimal("2")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("twice");

        assertThatThrownBy(() -> bom.addItem(UUID.randomUUID(), BigDecimal.ZERO))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> bom.addItem(UUID.randomUUID(), new BigDecimal("-1")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clearItemsEmptiesTheBom() {
        Bom bom = Bom.createForProduct(UUID.randomUUID());
        bom.addItem(UUID.randomUUID(), new BigDecimal("1"));
        bom.addItem(UUID.randomUUID(), new BigDecimal("2"));

        assertThat(bom.getItems()).hasSize(2);

        bom.clearItems();

        assertThat(bom.getItems()).isEmpty();
    }

    @Test
    void requiredQuantityMultipliesPerUnitByProductQuantity() {
        Bom bom = Bom.createForProduct(UUID.randomUUID());
        bom.addItem(UUID.randomUUID(), new BigDecimal("1.5"));

        var item = bom.getItems().get(0);

        assertThat(item.requiredQuantityFor(100))
            .isEqualByComparingTo(new BigDecimal("150.000000"));
    }
}
