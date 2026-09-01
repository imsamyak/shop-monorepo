package com.ecom.inventory.domain.entity;

import com.ecom.inventory.domain.exception.reservation.InvalidReservationQuantityException;
import com.ecom.inventory.domain.exception.reservation.InvalidReservationStateException;
import com.ecom.inventory.domain.exception.reservation.ReservationExpiredException;
import com.ecom.shared.exception.domain.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryReservationTest {

   @Test
   void shouldCreateReservation() {
      Instant expiresAt = Instant.now().plus(10, ChronoUnit.MINUTES);
      InventoryReservation reservation = InventoryReservation.of("checkout-1", 200L, 5, expiresAt);

      assertThat(reservation.getSkuId()).isEqualTo(200L);
      assertThat(reservation.getQuantity()).isEqualTo(5);
      assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
      assertThat(reservation.getExpiresAt()).isEqualTo(expiresAt);
      assertThat(reservation.getEntityName()).isEqualTo("InventoryReservation");
   }

   @Test
   void shouldRejectInvalidArgumentsOnCreate() {
      Instant future = Instant.now().plus(5, ChronoUnit.MINUTES);

      assertThatThrownBy(() -> InventoryReservation.of("checkout-1", null, 5, future))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("SKU ID cannot be null");

      assertThatThrownBy(() -> InventoryReservation.of("checkout-1", 100L, 0, future))
            .isInstanceOf(InvalidReservationQuantityException.class);

      assertThatThrownBy(() -> InventoryReservation.of("checkout-1", 100L, 5, null))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Expiration timestamp cannot be null");
   }

   @Test
   void shouldCheckExpirationCorrectly() {
      Instant past = Instant.now().minus(5, ChronoUnit.MINUTES);
      Instant future = Instant.now().plus(5, ChronoUnit.MINUTES);

      InventoryReservation expiredRes = InventoryReservation.of("checkout-1", 100L, 2, past);
      InventoryReservation activeRes = InventoryReservation.of("checkout-1", 100L, 2, future);

      assertThat(expiredRes.isExpired(Instant.now())).isTrue();
      assertThat(activeRes.isExpired(Instant.now())).isFalse();
   }

   @Test
   void shouldCommitSuccessfullyWhenPendingAndNotExpired() {
      Instant future = Instant.now().plus(10, ChronoUnit.MINUTES);
      InventoryReservation reservation = InventoryReservation.of("checkout-1", 100L, 2, future);

      reservation.commit(Instant.now());

      assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.COMMITTED);
   }

   @Test
   void shouldFailCommitWhenExpired() {
      Instant past = Instant.now().minus(1, ChronoUnit.MINUTES);
      InventoryReservation reservation = InventoryReservation.of("checkout-1", 100L, 2, past);

      assertThatThrownBy(() -> reservation.commit(Instant.now()))
            .isInstanceOf(ReservationExpiredException.class);

      // a failed commit must not change anything; the expiry sweep releases the stock later
      assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
   }

   @Test
   void shouldFailCommitWhenNotPending() {
      Instant future = Instant.now().plus(10, ChronoUnit.MINUTES);
      InventoryReservation reservation = InventoryReservation.of("checkout-1", 100L, 2, future);
      reservation.commit(Instant.now());

      assertThatThrownBy(() -> reservation.commit(Instant.now()))
            .isInstanceOf(InvalidReservationStateException.class)
            .hasMessageContaining("Cannot commit reservation");
   }

   @Test
   void shouldReleaseSuccessfullyWhenPending() {
      Instant future = Instant.now().plus(10, ChronoUnit.MINUTES);
      InventoryReservation reservation = InventoryReservation.of("checkout-1", 100L, 2, future);

      reservation.release();

      assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
   }

   @Test
   void shouldFailReleaseWhenAlreadyReleased() {
      Instant future = Instant.now().plus(10, ChronoUnit.MINUTES);
      InventoryReservation reservation = InventoryReservation.of("checkout-1", 100L, 2, future);
      reservation.release();

      assertThatThrownBy(() -> reservation.release())
            .isInstanceOf(InvalidReservationStateException.class)
            .hasMessageContaining("Cannot release reservation");
   }

   @Test
   void shouldExpireSuccessfully() {
      Instant past = Instant.now().minus(1, ChronoUnit.MINUTES);
      InventoryReservation reservation = InventoryReservation.of("checkout-1", 100L, 2, past);

      reservation.expire();

      assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
   }
}
