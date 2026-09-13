package com.ecom.inventory.domain.exception.reservation;

import com.ecom.inventory.domain.entity.ReservationStatus;
import com.ecom.shared.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationDomainExceptionTest {

   @Test
   void shouldCreateReservationNotFoundException() {
      UUID id = UUID.randomUUID();
      ReservationNotFoundException ex = ReservationNotFoundException.of(id);

      assertThat(ex.getReservationId()).isEqualTo(id);
      assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
      assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
      assertThat(ex.getMessage()).contains("InventoryReservation not found with identifier: " + id);
   }

   @Test
   void shouldCreateReservationExpiredException() {
      UUID id = UUID.randomUUID();
      Instant now = Instant.now();
      ReservationExpiredException ex = ReservationExpiredException.of(id, now);

      assertThat(ex.getReservationId()).isEqualTo(id);
      assertThat(ex.getExpiresAt()).isEqualTo(now);
      assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
      assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
   }

   @Test
   void shouldCreateInvalidReservationStateException() {
      UUID id = UUID.randomUUID();
      InvalidReservationStateException ex = InvalidReservationStateException.of(id, ReservationStatus.COMMITTED, "release");

      assertThat(ex.getReservationId()).isEqualTo(id);
      assertThat(ex.getCurrentStatus()).isEqualTo(ReservationStatus.COMMITTED);
      assertThat(ex.getAction()).isEqualTo("release");
      assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
      assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
   }

   @Test
   void shouldCreateInvalidReservationQuantityException() {
      InvalidReservationQuantityException ex = InvalidReservationQuantityException.of("Invalid quantity", -5);

      assertThat(ex.getQuantity()).isEqualTo(-5);
      assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
      assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
   }
}
