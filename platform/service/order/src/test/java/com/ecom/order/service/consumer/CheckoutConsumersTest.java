package com.ecom.order.service.consumer;

import com.ecom.contract.checkout.Checkout;
import com.ecom.inbox.InboxEvent;
import com.ecom.order.domain.entity.Order;
import com.ecom.order.domain.entity.OrderStatus;
import com.ecom.order.port.out.persistence.LoadOrderForUpdatePort;
import com.ecom.order.port.out.persistence.SaveOrderPort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The three consumers against an in-memory store: what an event does to the stored order, whatever order and however
 * often the events arrive.
 */
class CheckoutConsumersTest {

   /** The store both ports stand for; counts saves so a no-op delivery can be told from a change. */
   static class Store implements LoadOrderForUpdatePort, SaveOrderPort {
      final Map<UUID, Order> orders = new HashMap<>();
      int saves;

      @Override
      public Optional<Order> loadByIdForUpdate(UUID orderId) {
         return Optional.ofNullable(orders.get(orderId));
      }

      @Override
      public Order save(Order order) {
         saves++;
         orders.put(order.getId(), order);
         return order;
      }
   }

   private final Store store = new Store();
   private final CheckoutDraftedConsumer drafted = new CheckoutDraftedConsumer(store, store);
   private final CheckoutCommittedConsumer committed = new CheckoutCommittedConsumer(store, store);
   private final CheckoutCancelledConsumer cancelled = new CheckoutCancelledConsumer(store, store);

   private final UUID checkoutId = UUID.randomUUID();
   private final UUID userId = UUID.randomUUID();
   private final UUID sellerId = UUID.randomUUID();

   private List<Checkout.Item> items(int quantity) {
      return List.of(new Checkout.Item("7", quantity, new BigDecimal("5.00"), sellerId.toString()));
   }

   private void draft(long version, int quantity) {
      drafted.execute(new InboxEvent<>("m-" + UUID.randomUUID(),
            new Checkout.Drafted(checkoutId.toString(), userId.toString(), items(quantity), version)));
   }

   private void commit(long version, int quantity) {
      committed.execute(new InboxEvent<>("m-" + UUID.randomUUID(),
            new Checkout.Committed(checkoutId.toString(), userId.toString(), items(quantity), version)));
   }

   private void cancel(long version) {
      cancelled.execute(new InboxEvent<>("m-" + UUID.randomUUID(),
            new Checkout.Cancelled(checkoutId.toString(), "USER_CANCELLED", version)));
   }

   private Order order() {
      return store.orders.get(checkoutId);
   }

   @Test
   void inOrderDraftThenCommit() {
      draft(0, 2);
      commit(1, 2);

      assertThat(order().getStatus()).isEqualTo(OrderStatus.COMMITTED);
      assertThat(order().getCustomerId()).isEqualTo(userId);
      assertThat(order().getTotalAmount()).isEqualByComparingTo("10.00");
      assertThat(order().getItems().get(0).getSkuId()).isEqualTo(7L);
      assertThat(order().getItems().get(0).getSellerId()).isEqualTo(sellerId);
   }

   @Test
   void duplicateDeliveriesAreNotSavedAgain() {
      draft(0, 2);
      draft(0, 2);
      commit(1, 2);
      commit(1, 2);

      assertThat(store.saves).isEqualTo(2);
      assertThat(order().getStatus()).isEqualTo(OrderStatus.COMMITTED);
   }

   @Test
   void committedArrivingBeforeDraftedCreatesTheCommittedOrder() {
      commit(1, 3);
      draft(0, 3);

      assertThat(order().getStatus()).isEqualTo(OrderStatus.COMMITTED);
      assertThat(order().getAppliedVersion()).isEqualTo(1);
      assertThat(order().getTotalAmount()).isEqualByComparingTo("15.00");
      assertThat(store.saves).isEqualTo(1);
   }

   @Test
   void cancelledArrivingBeforeDraftedKeepsItCancelledAndLearnsTheCustomerLater() {
      cancel(1);
      assertThat(order().getStatus()).isEqualTo(OrderStatus.CANCELLED);
      assertThat(order().getCustomerId()).isNull();
      assertThat(order().getItems()).isEmpty();

      draft(0, 2);

      assertThat(order().getStatus()).isEqualTo(OrderStatus.CANCELLED);
      assertThat(order().getCustomerId()).isEqualTo(userId);
      assertThat(order().getItems()).hasSize(1);
   }

   @Test
   void aLateDraftedNeverOverwritesACancelledOrder() {
      draft(0, 2);
      cancel(1);
      draft(0, 9);

      assertThat(order().getStatus()).isEqualTo(OrderStatus.CANCELLED);
      assertThat(order().getItems().get(0).getQuantity()).isEqualTo(2);
   }

   @Test
   void cancelAfterCommitAndCommitAfterCancelAreIgnored() {
      draft(0, 2);
      commit(1, 2);
      cancel(2);
      assertThat(order().getStatus()).isEqualTo(OrderStatus.COMMITTED);

      UUID other = UUID.randomUUID();
      cancelled.execute(new InboxEvent<>("m-x", new Checkout.Cancelled(other.toString(), "EXPIRED", 1)));
      committed.execute(new InboxEvent<>("m-y", new Checkout.Committed(other.toString(), userId.toString(), items(1), 2)));
      assertThat(store.orders.get(other).getStatus()).isEqualTo(OrderStatus.CANCELLED);
   }

   @Test
   void aNewerDraftedRefreshesTheDraft() {
      draft(0, 2);
      draft(1, 4);

      assertThat(order().getStatus()).isEqualTo(OrderStatus.DRAFT);
      assertThat(order().getAppliedVersion()).isEqualTo(1);
      assertThat(order().getTotalAmount()).isEqualByComparingTo("20.00");
   }
}
