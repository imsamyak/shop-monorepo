package com.ecom.checkout.domain.entity;

import com.ecom.checkout.domain.exception.CartLimitExceededException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartTest {

   private final Cart cart = Cart.empty(UUID.randomUUID());

   private int quantityOf(long skuId) {
      return cart.getItems().stream().filter(i -> i.getSkuId() == skuId).findFirst().orElseThrow().getQuantity();
   }

   @Test
   void addingTheSameSkuAddsUpTheQuantities() {
      cart.add(7L, 2);
      cart.add(7L, 3);

      assertThat(cart.getItems()).hasSize(1);
      assertThat(quantityOf(7)).isEqualTo(5);
   }

   @Test
   void setReplacesTheQuantityAndStartsMissingLines() {
      cart.add(7L, 2);

      assertThat(cart.set(7L, 9)).isTrue();
      assertThat(cart.set(7L, 9)).as("same quantity, nothing changed").isFalse();
      assertThat(cart.set(8L, 1)).isTrue();

      assertThat(quantityOf(7)).isEqualTo(9);
      assertThat(cart.getItems()).hasSize(2);
   }

   @Test
   void removingAMissingSkuChangesNothing() {
      cart.add(7L, 2);

      assertThat(cart.remove(8L)).isFalse();
      assertThat(cart.remove(7L)).isTrue();
      assertThat(cart.getItems()).isEmpty();
   }

   @Test
   void removeAllTakesOnlyTheGivenSkus() {
      cart.add(1L, 1);
      cart.add(2L, 1);
      cart.add(3L, 1);

      assertThat(cart.removeAll(List.of(1L, 3L, 99L))).isTrue();

      assertThat(cart.getItems()).extracting(CartItem::getSkuId).containsExactly(2L);
   }

   @Test
   void clearingAnEmptyCartChangesNothing() {
      assertThat(cart.clear()).isFalse();
      cart.add(1L, 1);
      assertThat(cart.clear()).isTrue();
      assertThat(cart.getItems()).isEmpty();
   }

   @Test
   void aLineCannotGrowPastTheQuantityLimit() {
      cart.add(1L, 600);

      assertThatThrownBy(() -> cart.add(1L, 401)).isInstanceOf(CartLimitExceededException.class);
      assertThatThrownBy(() -> cart.set(1L, 1001)).isInstanceOf(CartLimitExceededException.class);
      assertThat(quantityOf(1)).isEqualTo(600);
   }

   @Test
   void aCartHoldsAtMostFiftyDifferentItems() {
      for (long sku = 1; sku <= Cart.MAX_ITEMS; sku++) {
         cart.add(sku, 1);
      }

      assertThatThrownBy(() -> cart.add(999L, 1)).isInstanceOf(CartLimitExceededException.class);
      assertThat(cart.add(1L, 1)).as("an existing line can still grow").isTrue();
   }

   @Test
   void everyChangeRefreshesUpdatedAt() throws Exception {
      java.time.Instant created = cart.getUpdatedAt();
      Thread.sleep(5);

      cart.add(1L, 1);

      assertThat(cart.getUpdatedAt()).isAfter(created);
   }
}
