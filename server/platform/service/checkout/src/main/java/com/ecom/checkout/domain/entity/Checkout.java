package com.ecom.checkout.domain.entity;

import com.ecom.checkout.domain.exception.CheckoutExpiredException;
import com.ecom.checkout.domain.exception.EmptyCheckoutException;
import com.ecom.checkout.domain.exception.InvalidCheckoutStateException;
import com.ecom.shared.entity.DomainEntity;
import com.ecom.shared.exception.domain.BusinessRuleException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * A buyer's reservation of items at server-side prices. The id is assigned up front because it is also the
 * idempotency reference used when reserving stock.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
   name = "checkouts",
   check = {
      @CheckConstraint(constraint = "total_amount >= 0", name = "chk_checkout_total_amount")
   },
   indexes = {
      @Index(name = "idx_checkout_status_expires", columnList = "status, expires_at")
   }
)
public class Checkout extends DomainEntity {

   @Id
   private UUID id;

   @Column(name = "customer_id", nullable = false, updatable = false)
   private UUID customerId;

   @Enumerated(EnumType.STRING)
   @Column(name = "status", nullable = false)
   private CheckoutStatus status;

   @Column(name = "total_amount", nullable = false)
   private BigDecimal totalAmount;

   @Column(name = "expires_at", nullable = false)
   private Instant expiresAt;

   @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
   @JoinColumn(name = "checkout_id", nullable = false)
   private final List<CheckoutItem> items = new ArrayList<>();

   public static Checkout draft(UUID id, UUID customerId, Instant expiresAt, List<CheckoutItem> items) {
      if (id == null) {
         throw BusinessRuleException.of("Checkout ID cannot be null");
      }
      if (customerId == null) {
         throw BusinessRuleException.of("Customer ID cannot be null");
      }
      if (expiresAt == null) {
         throw BusinessRuleException.of("Expiration timestamp cannot be null");
      }
      if (items == null || items.isEmpty()) {
         throw EmptyCheckoutException.of();
      }
      BigDecimal total = items.stream()
            .map(CheckoutItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

      Checkout checkout = new Checkout(id, customerId, CheckoutStatus.DRAFT, total, expiresAt);
      checkout.items.addAll(items);
      return checkout;
   }

   public boolean isExpired(Instant now) {
      return now.isAfter(this.expiresAt);
   }

   /** Commits a draft that has not expired. Callers handle the already-committed case before calling this. */
   public void commit(Instant now) {
      if (this.status != CheckoutStatus.DRAFT) {
         throw InvalidCheckoutStateException.of(this.id, this.status, "commit");
      }
      if (isExpired(now)) {
         throw CheckoutExpiredException.of(this.id, this.expiresAt);
      }
      this.status = CheckoutStatus.COMMITTED;
   }

   public void cancel() {
      if (this.status != CheckoutStatus.DRAFT) {
         throw InvalidCheckoutStateException.of(this.id, this.status, "cancel");
      }
      this.status = CheckoutStatus.CANCELLED;
   }

   /** Marks an overdue draft as expired. Returns whether anything changed. */
   public boolean expire(Instant now) {
      if (this.status == CheckoutStatus.DRAFT && isExpired(now)) {
         this.status = CheckoutStatus.EXPIRED;
         return true;
      }
      return false;
   }

   public List<CheckoutItem> getItems() {
      return Collections.unmodifiableList(this.items);
   }

   @Override
   public String getEntityName() {
      return "Checkout";
   }
}
