package com.ecom.checkout.domain.exception;

import com.ecom.checkout.domain.entity.CheckoutStatus;
import com.ecom.shared.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CheckoutExceptionTest {

   @Test
   void shouldCreateCheckoutNotFoundException() {
      UUID id = UUID.randomUUID();
      CheckoutNotFoundException ex = CheckoutNotFoundException.of(id);

      assertThat(ex.getCheckoutId()).isEqualTo(id);
      assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
      assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
      assertThat(ex.getMessage()).contains("Checkout not found with identifier: " + id);
   }

   @Test
   void shouldCreateInvalidCheckoutStateException() {
      UUID id = UUID.randomUUID();
      InvalidCheckoutStateException ex = InvalidCheckoutStateException.of(id, CheckoutStatus.COMMITTED, "commit");

      assertThat(ex.getCheckoutId()).isEqualTo(id);
      assertThat(ex.getCurrentStatus()).isEqualTo(CheckoutStatus.COMMITTED);
      assertThat(ex.getAction()).isEqualTo("commit");
      assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
      assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
      assertThat(ex.getDetails()).containsEntry("checkoutId", id.toString());
      assertThat(ex.getDetails()).containsEntry("currentStatus", "COMMITTED");
      assertThat(ex.getDetails()).containsEntry("action", "commit");
   }

   @Test
   void shouldCreateEmptyCheckoutException() {
      EmptyCheckoutException ex = EmptyCheckoutException.of();

      assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
      assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
      assertThat(ex.getMessage()).isEqualTo("Checkout must contain at least one item");
   }

   @Test
   void shouldCreateStockOperationRejectedException() {
      StockOperationRejectedException ex = StockOperationRejectedException.of("reserve stock", "Insufficient stock");

      assertThat(ex.getOperation()).isEqualTo("reserve stock");
      assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
      assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
      assertThat(ex.getDetails()).containsEntry("reason", "Insufficient stock");
   }

   @Test
   void shouldCreateSkuNotAvailableException() {
      SkuNotAvailableException ex = SkuNotAvailableException.of(7L);

      assertThat(ex.getSkuId()).isEqualTo(7L);
      assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
      assertThat(ex.getDetails()).containsEntry("skuId", "7");
   }
}
