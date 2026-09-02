package com.ecom.checkout.domain.entity;

import com.ecom.shared.entity.DomainEntity;
import com.ecom.shared.exception.domain.BusinessRuleException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
   name = "checkout_items",
   check = {
      @CheckConstraint(constraint = "quantity > 0", name = "chk_checkout_item_qty"),
      @CheckConstraint(constraint = "unit_price >= 0", name = "chk_checkout_item_unit_price"),
      @CheckConstraint(constraint = "subtotal >= 0", name = "chk_checkout_item_subtotal")
   }
)
public class CheckoutItem extends DomainEntity {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   @Column(name = "sku_id", nullable = false)
   private Long skuId;

   @Column(name = "seller_id", nullable = false)
   private UUID sellerId;

   @Column(name = "quantity", nullable = false)
   private int quantity;

   @Column(name = "unit_price", nullable = false)
   private BigDecimal unitPrice;

   @Column(name = "subtotal", nullable = false)
   private BigDecimal subtotal;

   @Column(name = "reservation_id")
   private UUID reservationId;

   public static CheckoutItem of(Long skuId, UUID sellerId, int quantity, BigDecimal unitPrice, UUID reservationId) {
      if (skuId == null) {
         throw BusinessRuleException.of("SKU ID cannot be null");
      }
      if (sellerId == null) {
         throw BusinessRuleException.of("Seller ID cannot be null");
      }
      if (quantity <= 0) {
         throw BusinessRuleException.of("Quantity must be greater than zero");
      }
      if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
         throw BusinessRuleException.of("Unit price must be non-negative");
      }
      BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
      return new CheckoutItem(null, skuId, sellerId, quantity, unitPrice, subtotal, reservationId);
   }

   @Override
   public String getEntityName() {
      return "CheckoutItem";
   }
}
