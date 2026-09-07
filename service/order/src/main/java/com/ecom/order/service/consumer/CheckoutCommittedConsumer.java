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
 * Commits the order of a checkout, creating it from the event if the drafted event has not arrived yet. Idempotent.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutCommittedConsumer implements InboxUseCase<Checkout.Committed> {

   private final LoadOrderForUpdatePort loadOrderForUpdatePort;
   private final SaveOrderPort saveOrderPort;

   @Override
   public Class<Checkout.Committed> subscribe() {
      return Checkout.Committed.class;
   }

   @Override
   @Transactional
   public Void execute(InboxEvent<Checkout.Committed> cmd) {
      Checkout.Committed event = cmd.event();
      UUID orderId = CheckoutEvents.orderId(event);

      Order order = loadOrderForUpdatePort.loadByIdForUpdate(orderId).orElseGet(() -> Order.unseen(orderId));
      if (order.applyCommitted(event.version(), CheckoutEvents.customerId(event.userId()), CheckoutEvents.items(event.items()))) {
         saveOrderPort.save(order);
         log.info("Order {} committed (version {})", orderId, event.version());
      } else {
         log.info("Ignored committed event of order {} (version {}, applied {})", orderId, event.version(), order.getAppliedVersion());
      }
      return null;
   }
}
