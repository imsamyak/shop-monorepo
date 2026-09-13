package com.ecom.inventory.domain.entity;

import com.ecom.inventory.domain.exception.reservation.InvalidReservationQuantityException;
import com.ecom.inventory.domain.exception.reservation.InvalidReservationStateException;
import com.ecom.inventory.domain.exception.reservation.ReservationExpiredException;
import com.ecom.shared.entity.DomainEntity;
import com.ecom.shared.exception.domain.BusinessRuleException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * A temporary hold on stock. {@code referenceId} identifies who asked (the checkout id), which makes reserving
 * idempotent: asking again for the same reference returns the existing holds instead of reserving twice.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
   name = "inventory_reservations",
   check = {
      @CheckConstraint(constraint = "quantity > 0", name = "chk_reservation_quantity")
   },
   uniqueConstraints = {
      @UniqueConstraint(columnNames = {"reference_id", "sku_id"}, name = "uc_reservation_reference_sku")
   },
   indexes = {
      @Index(name = "idx_reservation_status_expires", columnList = "status, expires_at"),
      @Index(name = "idx_reservation_sku_status", columnList = "sku_id, status")
   }
)
public class InventoryReservation extends DomainEntity {

   @Id
   @GeneratedValue(strategy = GenerationType.UUID)
   private UUID id;

   @Column(name = "reference_id", length = 100)
   private String referenceId;

   @Column(name = "sku_id", nullable = false)
   private Long skuId;

   @Column(name = "quantity", nullable = false)
   private int quantity;

   @Enumerated(EnumType.STRING)
   @Column(name = "status", nullable = false)
   private ReservationStatus status;

   @Column(name = "expires_at", nullable = false)
   private Instant expiresAt;

   @Builder
   public static InventoryReservation of(String referenceId, Long skuId, int quantity, Instant expiresAt) {
      if (skuId == null) {
         throw BusinessRuleException.of("SKU ID cannot be null");
      }
      if (quantity <= 0) {
         throw InvalidReservationQuantityException.of("Reservation quantity must be greater than zero", quantity);
      }
      if (expiresAt == null) {
         throw BusinessRuleException.of("Expiration timestamp cannot be null");
      }
      return new InventoryReservation(null, referenceId, skuId, quantity, ReservationStatus.PENDING, expiresAt);
   }

   public boolean isExpired(Instant now) {
      return this.expiresAt != null && now.isAfter(this.expiresAt);
   }

   public boolean isPending() {
      return this.status == ReservationStatus.PENDING;
   }

   /**
     * Commits a pending, unexpired reservation. The caller persists the state change; an expired reservation is not
     * modified here (the expiry sweep will release it) so a failed commit leaves everything untouched.
     */
   public void commit(Instant now) {
      if (this.status != ReservationStatus.PENDING) {
         throw InvalidReservationStateException.of(this.id, this.status, "commit");
      }
      if (isExpired(now)) {
         throw ReservationExpiredException.of(this.id, this.expiresAt);
      }
      this.status = ReservationStatus.COMMITTED;
   }

   public void release() {
      if (this.status != ReservationStatus.PENDING) {
         throw InvalidReservationStateException.of(this.id, this.status, "release");
      }
      this.status = ReservationStatus.RELEASED;
   }

   public void expire() {
      if (this.status == ReservationStatus.PENDING) {
         this.status = ReservationStatus.EXPIRED;
      }
   }

   @Override
   public String getEntityName() {
      return "InventoryReservation";
   }
}
