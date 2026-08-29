package com.ecom.inbox;

import com.ecom.inbox.internal.persistence.InboxEntity;
import com.ecom.inbox.internal.persistence.InboxRepository;
import com.ecom.shared.messaging.DomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = InboxIntegrationTest.TestConfig.class, properties = {
      "spring.datasource.url=jdbc:h2:mem:inbox_test_db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "inbox.deduplication-ttl-seconds=86400",
      "inbox.cleaner.enabled=false"
})
class InboxIntegrationTest {

   @Autowired
   private InboxListener inboxListener;

   @Autowired
   private InboxRepository inboxRepository;

   @Autowired
   private SampleOrderPlacedUseCase sampleUseCase;

   sealed interface Order extends DomainEvent {
      record Placed(String orderId, double price) implements Order {
         @Override
         public String aggregateId() {
            return orderId;
         }
      }
   }

   @Component
   static class SampleOrderPlacedUseCase implements InboxUseCase<Order.Placed> {
      final AtomicInteger executionCount = new AtomicInteger(0);

      public int getExecutionCount() {
         return executionCount.get();
      }

      @Override
      public Class<Order.Placed> subscribe() {
         return Order.Placed.class;
      }

      @Override
      public Void execute(InboxEvent<Order.Placed> cmd) {
         executionCount.incrementAndGet();
         return null;
      }
   }

   @Configuration
   @EnableAutoConfiguration
   @org.springframework.context.annotation.Import(SampleOrderPlacedUseCase.class)
   static class TestConfig {
   }

   @Test
   @DisplayName("End-to-end integration: listen routes to usecase, persists entity, and deduplicates identical messageId")
   void endToEndIngestionAndDeduplication() {
      String messageId = "msg-e2e-101";
      String payload = """
            {"orderId": "ord-99", "price": 49.99}
            """;

      // 1. First delivery
      inboxListener.listen(messageId, "Order.Placed", payload);

      assertEquals(1, sampleUseCase.getExecutionCount());

      Optional<InboxEntity> saved = inboxRepository.findByMessageId(messageId);
      assertTrue(saved.isPresent());
      assertEquals("order.placed", saved.get().getSignature());

      // 2. Redelivery of identical messageId (duplicate)
      inboxListener.listen(messageId, "Order.Placed", payload);

      // Count must remain 1 because aspect deduplicated it!
      assertEquals(1, sampleUseCase.getExecutionCount());
   }
}
