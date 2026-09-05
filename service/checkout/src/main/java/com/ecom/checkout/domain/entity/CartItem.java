package com.ecom.checkout.domain.entity;

import com.ecom.shared.entity.DomainEntity;
import com.ecom.shared.exception.domain.BusinessRuleException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
   name = "cart_items",
   check = {
      @CheckConstraint(constraint = "quantity > 0", name = "chk_cart_item_qty")
   },
   uniqueConstraints = {
      @UniqueConstraint(name = "uq_cart_item_sku", columnNames = {"cart_id", "sku_id"})
   }
)
public class CartItem extends DomainEntity {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   @Column(name = "sku_id", nullable = false)
   private Long skuId;

   @Column(name = "quantity", nullable = false)
   private int quantity;

   public static CartItem of(Long skuId, int quantity) {
      if (skuId == null) {
         throw BusinessRuleException.of("SKU ID cannot be null");
      }
      if (quantity <= 0) {
         throw BusinessRuleException.of("Quantity must be greater than zero");
      }
      return new CartItem(null, skuId, quantity);
   }

   void changeQuantity(int quantity) {
      this.quantity = quantity;
   }

   @Override
   public String getEntityName() {
      return "CartItem";
   }
}
