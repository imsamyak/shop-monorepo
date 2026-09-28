package com.ecom.order.domain.entity;

import com.ecom.shared.entity.DomainEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * An order as the checkout events describe it; the id is the checkout's id. The order only ever moves forward: an event
 * applies when its version is newer than the last one applied, and COMMITTED and CANCELLED are final. Events can arrive
 * late or out of order, so a cancellation may be the first thing seen about an order (it carries no customer or items);
 * a later, older event may then fill in what is still unknown but never changes the status.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "CustomerOrder") // "Order" is a query keyword
@Table(
   name = "orders",
   indexes = {
      @Index(name = "idx_order_customer", columnList = "customer_id"),
      @Index(name = "idx_order_created", columnList = "created_at")
   }
)
public class Order extends DomainEntity {

   /** Applied version of an order no event has been applied to yet. */
   private static final long NOTHING_APPLIED = -1;

   @Id
   private UUID id;

   /** Unknown (null) until an event that carries the customer has been seen. */
   @Column(name = "customer_id")
   private UUID customerId;

   @Enumerated(EnumType.STRING)
   @Column(name = "status", nullable = false)
   private OrderStatus status;

   @Column(name = "total_amount", nullable = false)
   private BigDecimal totalAmount;

   /** The highest checkout version applied so far. */
   @Column(name = "applied_version", nullable = false)
   private long appliedVersion;

   /** When the order was first seen. The same column the shared timestamp writes; this mapping only reads it. */
   @Column(name = "created_at", insertable = false, updatable = false)
   private LocalDateTime createdAt;

   @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
   @JoinColumn(name = "order_id", nullable = false)
   private final List<OrderItem> items = new ArrayList<>();

   /** An order nothing is known about yet; the first event applied to it always counts. */
   public static Order unseen(UUID id) {
      return new Order(id, null, OrderStatus.DRAFT, BigDecimal.ZERO, NOTHING_APPLIED, null);
   }

   /**
    * Applies a drafted event. Returns whether anything changed.
    */
   public boolean applyDrafted(long version, UUID customerId, List<OrderItem> items) {
      if (status.isTerminal() || version <= appliedVersion) {
         return fillMissing(customerId, items);
      }
      this.customerId = customerId;
      replaceItems(items);
      this.appliedVersion = version;
      return true;
   }

   /**
    * Applies a committed event. Returns whether anything changed.
    */
   public boolean applyCommitted(long version, UUID customerId, List<OrderItem> items) {
      if (status.isTerminal() || version <= appliedVersion) {
         return fillMissing(customerId, items);
      }
      this.customerId = customerId;
      replaceItems(items);
      this.status = OrderStatus.COMMITTED;
      this.appliedVersion = version;
      return true;
   }

   /**
    * Applies a cancelled event, which carries neither customer nor items: what is known stays. Returns whether
    * anything changed.
    */
   public boolean applyCancelled(long version) {
      if (status.isTerminal() || version <= appliedVersion) {
         return false;
      }
      this.status = OrderStatus.CANCELLED;
      this.appliedVersion = version;
      return true;
   }

   /**
    * An event that is not applied (old or after a final state) can still tell what a cancelled-first order does not
    * know. Never touches status or version.
    */
   private boolean fillMissing(UUID customerId, List<OrderItem> items) {
      boolean changed = false;
      if (this.customerId == null && customerId != null) {
         this.customerId = customerId;
         changed = true;
      }
      if (this.items.isEmpty() && items != null && !items.isEmpty()) {
         replaceItems(items);
         changed = true;
      }
      return changed;
   }

   private void replaceItems(List<OrderItem> items) {
      this.items.clear();
      if (items != null) {
         this.items.addAll(items);
      }
      this.totalAmount = this.items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
   }

   public List<OrderItem> getItems() {
      return Collections.unmodifiableList(this.items);
   }

   @Override
   public String getEntityName() {
      return "Order";
   }
}
