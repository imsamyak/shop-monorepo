package com.ecom.inventory.domain.entity;

import com.ecom.inventory.domain.exception.item.InsufficientStockException;
import com.ecom.inventory.domain.exception.reservation.InvalidReservationQuantityException;
import com.ecom.shared.exception.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryItemTest {

   private static final java.util.UUID SELLER = java.util.UUID.randomUUID();

   @Test
   void shouldCreateInventoryItem() {
      InventoryItem item = InventoryItem.of(100L, SELLER, 50);

      assertThat(item.getSkuId()).isEqualTo(100L);
      assertThat(item.getTotalQuantity()).isEqualTo(50);
      assertThat(item.getReservedQuantity()).isEqualTo(0);
      assertThat(item.getAvailableQuantity()).isEqualTo(50);
      assertThat(item.getEntityName()).isEqualTo("InventoryItem");
   }

   @Test
   void shouldRejectNegativeInitialQuantity() {
      assertThatThrownBy(() -> InventoryItem.of(100L, SELLER, -5))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Total quantity cannot be negative");
   }

   @Test
   void shouldRejectNullSkuId() {
      assertThatThrownBy(() -> InventoryItem.of(null, SELLER, 10))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("SKU ID cannot be null");
   }

   @Test
   void shouldReserveStockSuccessfully() {
      InventoryItem item = InventoryItem.of(100L, SELLER, 50);

      item.reserve(20);

      assertThat(item.getReservedQuantity()).isEqualTo(20);
      assertThat(item.getAvailableQuantity()).isEqualTo(30);
      assertThat(item.getTotalQuantity()).isEqualTo(50);
   }

   @Test
   void shouldFailReserveWhenInsufficientStock() {
      InventoryItem item = InventoryItem.of(100L, SELLER, 10);

      assertThatThrownBy(() -> item.reserve(15))
            .isInstanceOf(InsufficientStockException.class)
            .hasMessageContaining("Insufficient stock for SKU 100");
   }

   @Test
   void shouldFailReserveWhenZeroOrNegativeQuantity() {
      InventoryItem item = InventoryItem.of(100L, SELLER, 10);

      assertThatThrownBy(() -> item.reserve(0))
            .isInstanceOf(InvalidReservationQuantityException.class);

      assertThatThrownBy(() -> item.reserve(-1))
            .isInstanceOf(InvalidReservationQuantityException.class);
   }

   @Test
   void shouldCommitStockSuccessfully() {
      InventoryItem item = InventoryItem.of(100L, SELLER, 50);
      item.reserve(20);

      item.commit(20);

      assertThat(item.getTotalQuantity()).isEqualTo(30);
      assertThat(item.getReservedQuantity()).isEqualTo(0);
      assertThat(item.getAvailableQuantity()).isEqualTo(30);
   }

   @Test
   void shouldFailCommitWhenQuantityExceedsReserved() {
      InventoryItem item = InventoryItem.of(100L, SELLER, 50);
      item.reserve(10);

      assertThatThrownBy(() -> item.commit(15))
            .isInstanceOf(InsufficientStockException.class);
   }

   @Test
   void shouldReleaseStockSuccessfully() {
      InventoryItem item = InventoryItem.of(100L, SELLER, 50);
      item.reserve(20);

      item.release(15);

      assertThat(item.getReservedQuantity()).isEqualTo(5);
      assertThat(item.getAvailableQuantity()).isEqualTo(45);
      assertThat(item.getTotalQuantity()).isEqualTo(50);
   }

   @Test
   void shouldFailReleaseWhenQuantityExceedsReserved() {
      InventoryItem item = InventoryItem.of(100L, SELLER, 50);
      item.reserve(5);

      assertThatThrownBy(() -> item.release(10))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Cannot release more stock than currently reserved");
   }

   @Test
   void shouldAdjustTotalQuantitySuccessfully() {
      InventoryItem item = InventoryItem.of(100L, SELLER, 50);
      item.reserve(10);

      item.adjustTotalQuantity(80);

      assertThat(item.getTotalQuantity()).isEqualTo(80);
      assertThat(item.getReservedQuantity()).isEqualTo(10);
      assertThat(item.getAvailableQuantity()).isEqualTo(70);
   }

   @Test
   void shouldFailAdjustTotalQuantityWhenLessThanReserved() {
      InventoryItem item = InventoryItem.of(100L, SELLER, 50);
      item.reserve(30);

      assertThatThrownBy(() -> item.adjustTotalQuantity(20))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Total quantity cannot be less than reserved quantity");
   }

   @Test
   void shouldAddStockSuccessfully() {
      InventoryItem item = InventoryItem.of(100L, SELLER, 50);
      item.reserve(10);

      item.addStock(25);

      assertThat(item.getTotalQuantity()).isEqualTo(75);
      assertThat(item.getReservedQuantity()).isEqualTo(10);
      assertThat(item.getAvailableQuantity()).isEqualTo(65);
   }

   @Test
   void shouldFailAddStockWhenZeroOrNegative() {
      InventoryItem item = InventoryItem.of(100L, SELLER, 50);

      assertThatThrownBy(() -> item.addStock(0))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Add stock quantity must be greater than zero");

      assertThatThrownBy(() -> item.addStock(-5))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Add stock quantity must be greater than zero");
   }

   @Test
   void shouldRemoveStockSuccessfully() {
      InventoryItem item = InventoryItem.of(100L, SELLER, 50);
      item.reserve(10);

      item.removeStock(15);

      assertThat(item.getTotalQuantity()).isEqualTo(35);
      assertThat(item.getReservedQuantity()).isEqualTo(10);
      assertThat(item.getAvailableQuantity()).isEqualTo(25);
   }

   @Test
   void shouldFailRemoveStockWhenExceedingAvailable() {
      InventoryItem item = InventoryItem.of(100L, SELLER, 50);
      item.reserve(40);

      assertThatThrownBy(() -> item.removeStock(15))
            .isInstanceOf(InsufficientStockException.class)
            .hasMessageContaining("Insufficient stock for SKU 100: requested 15, available 10");
   }

   @Test
   void shouldFailRemoveStockWhenZeroOrNegative() {
      InventoryItem item = InventoryItem.of(100L, SELLER, 50);

      assertThatThrownBy(() -> item.removeStock(0))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Remove stock quantity must be greater than zero");

      assertThatThrownBy(() -> item.removeStock(-10))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Remove stock quantity must be greater than zero");
   }

   @Test
   void shouldRequireASeller() {
      assertThatThrownBy(() -> InventoryItem.of(100L, null, 10)).isInstanceOf(BusinessRuleException.class);
   }

   @Test
   void shouldRefuseToOverflowTheTotal() {
      InventoryItem item = InventoryItem.of(100L, SELLER, Integer.MAX_VALUE - 1);

      assertThatThrownBy(() -> item.addStock(5)).isInstanceOf(BusinessRuleException.class);
      assertThat(item.getTotalQuantity()).isEqualTo(Integer.MAX_VALUE - 1);
   }
}
