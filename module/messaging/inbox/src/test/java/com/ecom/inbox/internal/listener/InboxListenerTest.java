package com.ecom.inbox.internal.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ecom.inbox.InboxEvent;
import com.ecom.inbox.InboxUseCase;
import com.ecom.shared.messaging.DomainEvent;
import com.ecom.shared.messaging.DomainEventRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class InboxListenerTest {

   private final ObjectMapper objectMapper = new ObjectMapper();
   private DomainEventRegistry eventRegistry;
   private DefaultInboxListener inboxListener;

   sealed interface Order extends DomainEvent {
      record Placed(String orderId, double amount) implements Order {
         @Override
         public String aggregateId() {
            return orderId;
         }
      }

      record Cancelled(String orderId) implements Order {
         @Override
         public String aggregateId() {
            return orderId;
         }
      }
   }

   @BeforeEach
   void setUp() {
      eventRegistry = new DomainEventRegistry();
      inboxListener = new DefaultInboxListener(eventRegistry, objectMapper);
   }

   @Test
   @DisplayName("Dispatches matching domain event to registered InboxUseCase")
   void dispatchesToMatchingUseCase() {
      AtomicReference<InboxEvent<Order.Placed>> received = new AtomicReference<>();

      InboxUseCase<Order.Placed> useCase = new InboxUseCase<>() {
         @Override
         public Class<Order.Placed> subscribe() {
            return Order.Placed.class;
         }

         @Override
         public Void execute(InboxEvent<Order.Placed> cmd) {
            received.set(cmd);
            return null;
         }
      };

      inboxListener.registerUseCase(useCase);

      String payload = """
            {"orderId": "ord-1", "amount": 99.5}
            """;

      inboxListener.listen("msg-101", "order.placed", payload);

      assertNotNull(received.get());
      assertEquals("msg-101", received.get().messageId());
      assertEquals("ord-1", received.get().event().orderId());
      assertEquals(99.5, received.get().event().amount());
   }

   @Test
   @DisplayName("Ignores event with unknown signature safely")
   void ignoresUnknownSignature() {
      AtomicBoolean called = new AtomicBoolean(false);

      InboxUseCase<Order.Placed> useCase = new InboxUseCase<>() {
         @Override
         public Class<Order.Placed> subscribe() {
            return Order.Placed.class;
         }

         @Override
         public Void execute(InboxEvent<Order.Placed> cmd) {
            called.set(true);
            return null;
         }
      };

      inboxListener.registerUseCase(useCase);

      inboxListener.listen("msg-999", "Customer.Registered", "{}");

      assertFalse(called.get());
   }

   @Test
   @DisplayName("Throws IllegalArgumentException when payload JSON is malformed")
   void throwsOnMalformedPayload() {
      InboxUseCase<Order.Placed> useCase = new InboxUseCase<>() {
         @Override
         public Class<Order.Placed> subscribe() {
            return Order.Placed.class;
         }

         @Override
         public Void execute(InboxEvent<Order.Placed> cmd) {
            return null;
         }
      };

      inboxListener.registerUseCase(useCase);

      assertThrows(IllegalArgumentException.class, () ->
            inboxListener.listen("msg-1", "Order.Placed", "not-a-json"));
   }

   @Test
   @DisplayName("Null arguments throw NullPointerException")
   void nullArgumentsThrowNullPointerException() {
      assertThrows(NullPointerException.class, () -> inboxListener.listen(null, "Order.Placed", "{}"));
      assertThrows(NullPointerException.class, () -> inboxListener.listen("msg-1", null, "{}"));
      assertThrows(NullPointerException.class, () -> inboxListener.listen("msg-1", "Order.Placed", null));
      assertThrows(NullPointerException.class, () -> inboxListener.registerUseCase(null));
   }
}
