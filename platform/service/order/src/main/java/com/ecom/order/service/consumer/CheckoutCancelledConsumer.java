package com.ecom.order.service.consumer;

import com.ecom.contract.checkout.Checkout;
import com.ecom.inbox.InboxEvent;
import com.ecom.inbox.InboxUseCase;
import com.ecom.order.domain.entity.Order;
import com.ecom.order.port.out.persistence.LoadOrderForUpdatePort;
import com.ecom.order.port.out.persistence.SaveOrderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Cancels the order of a checkout (cancelled by the user or expired). The event carries no customer or items, so an
 * order first seen through it has neither until an older event fills them in. Idempotent.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutCancelledConsumer implements InboxUseCase<Checkout.Cancelled> {

   private final LoadOrderForUpdatePort loadOrderForUpdatePort;
   private final SaveOrderPort saveOrderPort;

   @Override
   public Class<Checkout.Cancelled> subscribe() {
      return Checkout.Cancelled.class;
   }

   @Override
   @Transactional
   public Void execute(InboxEvent<Checkout.Cancelled> cmd) {
      Checkout.Cancelled event = cmd.event();
      UUID orderId = CheckoutEvents.orderId(event);

      Order order = loadOrderForUpdatePort.loadByIdForUpdate(orderId).orElseGet(() -> Order.unseen(orderId));
      if (order.applyCancelled(event.version())) {
         saveOrderPort.save(order);
         log.info("Order {} cancelled: {} (version {})", orderId, event.reason(), event.version());
      } else {
         log.info("Ignored cancelled event of order {} (version {}, applied {})", orderId, event.version(), order.getAppliedVersion());
      }
      return null;
   }
}
