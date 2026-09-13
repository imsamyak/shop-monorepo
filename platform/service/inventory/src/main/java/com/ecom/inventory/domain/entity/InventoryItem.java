package com.ecom.inventory.domain.entity;

import com.ecom.inventory.domain.exception.item.InsufficientStockException;
import com.ecom.inventory.domain.exception.reservation.InvalidReservationQuantityException;
import com.ecom.shared.entity.DomainEntity;
import com.ecom.shared.exception.domain.BusinessRuleException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Stock level of one SKU. The owning seller is copied from the SKU when the item is created (from the
 * {@code Sku.Created} event) and decides who may change the stock.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
   name = "inventory_items",
   check = {
      @CheckConstraint(constraint = "total_quantity >= 0", name = "chk_inventory_total_qty"),
      @CheckConstraint(constraint = "reserved_quantity >= 0", name = "chk_inventory_reserved_qty"),
      @CheckConstraint(constraint = "total_quantity >= reserved_quantity", name = "chk_inventory_qty_valid")
   },
   uniqueConstraints = {
      @UniqueConstraint(columnNames = {"sku_id"}, name = "uc_inventory_items_sku_id")
   }
)
public class InventoryItem extends DomainEntity {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   @Column(name = "sku_id", nullable = false)
   private Long skuId;

   @Column(name = "seller_id", nullable = false, updatable = false)
   private UUID sellerId;

   @Column(name = "total_quantity", nullable = false)
   private int totalQuantity;

   @Column(name = "reserved_quantity", nullable = false)
   private int reservedQuantity;

   @Builder
   public static InventoryItem of(Long skuId, UUID sellerId, int totalQuantity) {
      if (skuId == null) {
         throw BusinessRuleException.of("SKU ID cannot be null");
      }
      if (sellerId == null) {
         throw BusinessRuleException.of("Seller ID cannot be null");
      }
      if (totalQuantity < 0) {
         throw BusinessRuleException.of("Total quantity cannot be negative");
      }
      return new InventoryItem(null, skuId, sellerId, totalQuantity, 0);
   }

   public int getAvailableQuantity() {
      return Math.max(0, this.totalQuantity - this.reservedQuantity);
   }

   public InventoryItem reserve(int quantity) {
      if (quantity <= 0) {
         throw InvalidReservationQuantityException.of("Reservation quantity must be greater than zero", quantity);
      }
      int available = getAvailableQuantity();
      if (available < quantity) {
         throw InsufficientStockException.of(this.skuId, quantity, available);
      }
      this.reservedQuantity += quantity;
      return this;
   }

   public InventoryItem commit(int quantity) {
      if (quantity <= 0) {
         throw InvalidReservationQuantityException.of("Commit quantity must be greater than zero", quantity);
      }
      if (this.reservedQuantity < quantity) {
         throw InsufficientStockException.of(this.skuId, quantity, this.reservedQuantity);
      }
      if (this.totalQuantity < quantity) {
         throw InsufficientStockException.of(this.skuId, quantity, this.totalQuantity);
      }
      this.reservedQuantity -= quantity;
      this.totalQuantity -= quantity;
      return this;
   }

   public InventoryItem release(int quantity) {
      if (quantity <= 0) {
         throw InvalidReservationQuantityException.of("Release quantity must be greater than zero", quantity);
      }
      if (this.reservedQuantity < quantity) {
         throw BusinessRuleException.of("Cannot release more stock than currently reserved");
      }
      this.reservedQuantity -= quantity;
      return this;
   }

   public InventoryItem adjustTotalQuantity(int newTotal) {
      if (newTotal < 0) {
         throw BusinessRuleException.of("Total quantity cannot be negative");
      }
      if (newTotal < this.reservedQuantity) {
         throw BusinessRuleException.of("Total quantity cannot be less than reserved quantity");
      }
      this.totalQuantity = newTotal;
      return this;
   }

   public InventoryItem addStock(int quantity) {
      if (quantity <= 0) {
         throw BusinessRuleException.of("Add stock quantity must be greater than zero");
      }
      try {
         this.totalQuantity = Math.addExact(this.totalQuantity, quantity);
      } catch (ArithmeticException overflow) {
         throw BusinessRuleException.of("Total quantity would exceed the maximum supported stock");
      }
      return this;
   }

   public InventoryItem removeStock(int quantity) {
      if (quantity <= 0) {
         throw BusinessRuleException.of("Remove stock quantity must be greater than zero");
      }
      int available = getAvailableQuantity();
      if (available < quantity) {
         throw InsufficientStockException.of(this.skuId, quantity, available);
      }
      this.totalQuantity -= quantity;
      return this;
   }

   @Override
   public String getEntityName() {
      return "InventoryItem";
   }
}
