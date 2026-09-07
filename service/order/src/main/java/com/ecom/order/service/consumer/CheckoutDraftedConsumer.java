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
 * Creates the order of a drafted checkout, or refreshes it if the event is newer than what is applied. Idempotent, and
 * a late drafted event never undoes a committed or cancelled order.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutDraftedConsumer implements InboxUseCase<Checkout.Drafted> {

   private final LoadOrderForUpdatePort loadOrderForUpdatePort;
   private final SaveOrderPort saveOrderPort;

   @Override
   public Class<Checkout.Drafted> subscribe() {
      return Checkout.Drafted.class;
   }

   @Override
   @Transactional
   public Void execute(InboxEvent<Checkout.Drafted> cmd) {
      Checkout.Drafted event = cmd.event();
      UUID orderId = CheckoutEvents.orderId(event);

      Order order = loadOrderForUpdatePort.loadByIdForUpdate(orderId).orElseGet(() -> Order.unseen(orderId));
      if (order.applyDrafted(event.version(), CheckoutEvents.customerId(event.userId()), CheckoutEvents.items(event.items()))) {
         saveOrderPort.save(order);
         log.info("Order {} drafted (version {})", orderId, event.version());
      } else {
         log.info("Ignored drafted event of order {} (version {}, applied {})", orderId, event.version(), order.getAppliedVersion());
      }
      return null;
   }
}
