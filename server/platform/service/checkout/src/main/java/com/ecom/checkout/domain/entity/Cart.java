package com.ecom.checkout.domain.entity;

import com.ecom.checkout.domain.exception.CartLimitExceededException;
import com.ecom.shared.entity.DomainEntity;
import com.ecom.shared.exception.domain.BusinessRuleException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A customer's cart: SKUs and quantities only, never prices (those are looked up when a checkout is drafted). There is
 * one per customer. Every mutator returns whether anything changed, so callers save only real edits; the version
 * guards concurrent edits.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
   name = "carts",
   uniqueConstraints = {
      @UniqueConstraint(name = "uq_cart_customer", columnNames = "customer_id")
   }
)
public class Cart extends DomainEntity {

   public static final int MAX_ITEMS = 50;
   public static final int MAX_QUANTITY = 1000;

   @Id
   private UUID id;

   @Column(name = "customer_id", nullable = false, updatable = false)
   private UUID customerId;

   @Column(name = "updated_at", nullable = false)
   private Instant updatedAt;

   @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
   @JoinColumn(name = "cart_id", nullable = false)
   private final List<CartItem> items = new ArrayList<>();

   public static Cart empty(UUID customerId) {
      if (customerId == null) {
         throw BusinessRuleException.of("Customer ID cannot be null");
      }
      Cart cart = new Cart();
      cart.id = UUID.randomUUID();
      cart.customerId = customerId;
      cart.updatedAt = Instant.now();
      return cart;
   }

   /** Adds to the quantity already in the cart for the SKU, or starts a line for it. */
   public boolean add(Long skuId, int quantity) {
      requirePositive(quantity);
      Optional<CartItem> line = find(skuId);
      if (line.isPresent()) {
         line.get().changeQuantity(requireWithinLimit(skuId, line.get().getQuantity() + (long) quantity));
      } else {
         append(skuId, quantity);
      }
      return touch();
   }

   /** Sets the quantity of the SKU, starting a line for it if there is none. */
   public boolean set(Long skuId, int quantity) {
      requirePositive(quantity);
      Optional<CartItem> line = find(skuId);
      if (line.isPresent()) {
         if (line.get().getQuantity() == quantity) {
            return false;
         }
         line.get().changeQuantity(requireWithinLimit(skuId, quantity));
      } else {
         append(skuId, quantity);
      }
      return touch();
   }

   public boolean remove(Long skuId) {
      return removeAll(Collections.singleton(skuId));
   }

   public boolean removeAll(Collection<Long> skuIds) {
      return items.removeIf(i -> skuIds.contains(i.getSkuId())) && touch();
   }

   public boolean clear() {
      if (items.isEmpty()) {
         return false;
      }
      items.clear();
      return touch();
   }

   public List<CartItem> getItems() {
      return Collections.unmodifiableList(this.items);
   }

   @Override
   public String getEntityName() {
      return "Cart";
   }

   private Optional<CartItem> find(Long skuId) {
      if (skuId == null) {
         throw BusinessRuleException.of("SKU ID cannot be null");
      }
      return items.stream().filter(i -> i.getSkuId().equals(skuId)).findFirst();
   }

   private void append(Long skuId, int quantity) {
      if (items.size() >= MAX_ITEMS) {
         throw CartLimitExceededException.tooManyItems(MAX_ITEMS);
      }
      items.add(CartItem.of(skuId, requireWithinLimit(skuId, quantity)));
   }

   private static int requireWithinLimit(Long skuId, long quantity) {
      if (quantity > MAX_QUANTITY) {
         throw CartLimitExceededException.quantityTooLarge(skuId, MAX_QUANTITY);
      }
      return (int) quantity;
   }

   private static void requirePositive(int quantity) {
      if (quantity <= 0) {
         throw BusinessRuleException.of("Quantity must be greater than zero");
      }
   }

   private boolean touch() {
      this.updatedAt = Instant.now();
      return true;
   }
}
