package com.textile.manufacturing.inventory;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.textile.manufacturing.inventory.domain.StockBalance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryDomainTests {

    @Test
    void receiveAddsToOnHand() {
        StockBalance balance = StockBalance.createEmpty(UUID.randomUUID());

        balance.receive(new BigDecimal("100"));
        balance.receive(new BigDecimal("50"));

        assertThat(balance.getOnHandQuantity()).isEqualByComparingTo(new BigDecimal("150.000000"));
        assertThat(balance.availableQuantity()).isEqualByComparingTo(new BigDecimal("150.000000"));
    }

    @Test
    void adjustCannotMakeOnHandNegative() {
        StockBalance balance = StockBalance.createEmpty(UUID.randomUUID());
        balance.receive(new BigDecimal("100"));

        balance.adjustOnHandBy(new BigDecimal("-10"));

        assertThatThrownBy(() -> balance.adjustOnHandBy(new BigDecimal("-91")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("negative");

        assertThat(balance.getOnHandQuantity()).isEqualByComparingTo(new BigDecimal("90.000000"));
    }

    @Test
    void reserveChecksAvailableNotOnHand() {
        StockBalance balance = StockBalance.createEmpty(UUID.randomUUID());
        balance.receive(new BigDecimal("100"));
        balance.reserve(new BigDecimal("80"));

        assertThat(balance.availableQuantity()).isEqualByComparingTo(new BigDecimal("20.000000"));

        assertThatThrownBy(() -> balance.reserve(new BigDecimal("21")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Insufficient available");

        assertThat(balance.getReservedQuantity()).isEqualByComparingTo(new BigDecimal("80.000000"));
    }

    @Test
    void releaseCannotExceedReserved() {
        StockBalance balance = StockBalance.createEmpty(UUID.randomUUID());
        balance.receive(new BigDecimal("100"));
        balance.reserve(new BigDecimal("50"));

        balance.release(new BigDecimal("50"));

        assertThatThrownBy(() -> balance.release(new BigDecimal("1")))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(balance.getReservedQuantity()).isEqualByComparingTo(BigDecimal.ZERO.setScale(6));
        assertThat(balance.getOnHandQuantity()).isEqualByComparingTo(new BigDecimal("100.000000"));
    }

    @Test
    void consumeReducesBothOnHandAndReserved() {
        StockBalance balance = StockBalance.createEmpty(UUID.randomUUID());
        balance.receive(new BigDecimal("100"));
        balance.reserve(new BigDecimal("80"));

        balance.consume(new BigDecimal("80"));

        assertThat(balance.getOnHandQuantity()).isEqualByComparingTo(new BigDecimal("20.000000"));
        assertThat(balance.getReservedQuantity()).isEqualByComparingTo(BigDecimal.ZERO.setScale(6));
        assertThat(balance.availableQuantity()).isEqualByComparingTo(new BigDecimal("20.000000"));
    }
}
