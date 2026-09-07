package com.ecom.order.domain.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

   private static final UUID CUSTOMER = UUID.randomUUID();

   private static List<OrderItem> items(int quantity) {
      return List.of(OrderItem.of(1L, UUID.randomUUID(), quantity, new BigDecimal("10.50")));
   }

   private static Order unseen() {
      return Order.unseen(UUID.randomUUID());
   }

   @Test
   void draftedCreatesADraftWithItemsAndTotal() {
      Order order = unseen();

      assertThat(order.applyDrafted(0, CUSTOMER, items(2))).isTrue();

      assertThat(order.getStatus()).isEqualTo(OrderStatus.DRAFT);
      assertThat(order.getCustomerId()).isEqualTo(CUSTOMER);
      assertThat(order.getAppliedVersion()).isZero();
      assertThat(order.getItems()).hasSize(1);
      assertThat(order.getTotalAmount()).isEqualByComparingTo("21.00");
   }

   @Test
   void theSameVersionAgainChangesNothing() {
      Order order = unseen();
      order.applyDrafted(3, CUSTOMER, items(2));

      assertThat(order.applyDrafted(3, CUSTOMER, items(5))).isFalse();

      assertThat(order.getItems().get(0).getQuantity()).isEqualTo(2);
   }

   @Test
   void olderDraftedDoesNotOverwriteANewerOne() {
      Order order = unseen();
      order.applyDrafted(4, CUSTOMER, items(2));

      assertThat(order.applyDrafted(2, CUSTOMER, items(9))).isFalse();

      assertThat(order.getAppliedVersion()).isEqualTo(4);
      assertThat(order.getItems().get(0).getQuantity()).isEqualTo(2);
   }

   @Test
   void newerDraftedReplacesTheItems() {
      Order order = unseen();
      order.applyDrafted(1, CUSTOMER, items(2));

      assertThat(order.applyDrafted(2, CUSTOMER, items(3))).isTrue();

      assertThat(order.getItems()).hasSize(1);
      assertThat(order.getTotalAmount()).isEqualByComparingTo("31.50");
   }

   @Test
   void committedMovesADraftToCommitted() {
      Order order = unseen();
      order.applyDrafted(0, CUSTOMER, items(2));

      assertThat(order.applyCommitted(1, CUSTOMER, items(2))).isTrue();

      assertThat(order.getStatus()).isEqualTo(OrderStatus.COMMITTED);
      assertThat(order.getAppliedVersion()).isEqualTo(1);
   }

   @Test
   void committedBeforeDraftedStillCreatesTheOrderAndALateDraftedChangesNothing() {
      Order order = unseen();
      order.applyCommitted(1, CUSTOMER, items(2));

      assertThat(order.applyDrafted(0, CUSTOMER, items(7))).isFalse();

      assertThat(order.getStatus()).isEqualTo(OrderStatus.COMMITTED);
      assertThat(order.getAppliedVersion()).isEqualTo(1);
      assertThat(order.getItems().get(0).getQuantity()).isEqualTo(2);
   }

   @Test
   void cancelledMovesADraftToCancelledAndKeepsWhatIsKnown() {
      Order order = unseen();
      order.applyDrafted(0, CUSTOMER, items(2));

      assertThat(order.applyCancelled(1)).isTrue();

      assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
      assertThat(order.getCustomerId()).isEqualTo(CUSTOMER);
      assertThat(order.getItems()).hasSize(1);
      assertThat(order.getTotalAmount()).isEqualByComparingTo("21.00");
   }

   @Test
   void cancelledBeforeDraftedCreatesAnOrderWithoutCustomerAndALateDraftedOnlyFillsTheGaps() {
      Order order = unseen();
      order.applyCancelled(1);

      assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
      assertThat(order.getCustomerId()).isNull();
      assertThat(order.getItems()).isEmpty();
      assertThat(order.getTotalAmount()).isEqualByComparingTo("0");

      assertThat(order.applyDrafted(0, CUSTOMER, items(2))).isTrue();

      assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
      assertThat(order.getAppliedVersion()).isEqualTo(1);
      assertThat(order.getCustomerId()).isEqualTo(CUSTOMER);
      assertThat(order.getItems()).hasSize(1);
      assertThat(order.getTotalAmount()).isEqualByComparingTo("21.00");
   }

   @Test
   void terminalStatesAreFinal() {
      Order committed = unseen();
      committed.applyCommitted(1, CUSTOMER, items(2));
      assertThat(committed.applyCancelled(5)).isFalse();
      assertThat(committed.applyCommitted(6, CUSTOMER, items(3))).isFalse();
      assertThat(committed.getStatus()).isEqualTo(OrderStatus.COMMITTED);

      Order cancelled = unseen();
      cancelled.applyCancelled(1);
      assertThat(cancelled.applyCommitted(5, CUSTOMER, items(2))).isTrue(); // only fills the unknowns
      assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
      assertThat(cancelled.getAppliedVersion()).isEqualTo(1);
      assertThat(cancelled.applyCommitted(6, CUSTOMER, items(3))).isFalse();
   }

   @Test
   void duplicateCancelledChangesNothing() {
      Order order = unseen();
      order.applyCancelled(2);

      assertThat(order.applyCancelled(2)).isFalse();
      assertThat(order.applyCancelled(1)).isFalse();
   }
}
