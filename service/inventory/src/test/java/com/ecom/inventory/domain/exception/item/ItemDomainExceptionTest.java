package com.ecom.inventory.domain.exception.item;

import com.ecom.shared.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ItemDomainExceptionTest {

   @Test
   void shouldCreateInventoryNotFoundException() {
      InventoryNotFoundException ex = InventoryNotFoundException.of(101L);

      assertThat(ex.getSkuId()).isEqualTo(101L);
      assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
      assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
      assertThat(ex.getMessage()).contains("InventoryItem not found with identifier: 101");
   }

   @Test
   void shouldCreateInsufficientStockException() {
      InsufficientStockException ex = InsufficientStockException.of(101L, 10, 5);

      assertThat(ex.getSkuId()).isEqualTo(101L);
      assertThat(ex.getRequested()).isEqualTo(10);
      assertThat(ex.getAvailable()).isEqualTo(5);
      assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
      assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
      assertThat(ex.getDetails()).containsEntry("skuId", "101");
      assertThat(ex.getDetails()).containsEntry("requested", 10);
      assertThat(ex.getDetails()).containsEntry("available", 5);
   }
}
