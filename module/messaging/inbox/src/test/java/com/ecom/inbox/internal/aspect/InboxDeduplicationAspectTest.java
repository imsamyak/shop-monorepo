package com.ecom.inbox.internal.aspect;

import com.ecom.inbox.InboxEvent;
import com.ecom.inbox.InboxUseCase;
import com.ecom.inbox.internal.config.InboxProperties;
import com.ecom.inbox.internal.persistence.InboxEntity;
import com.ecom.inbox.internal.persistence.InboxRepository;
import com.ecom.shared.messaging.DomainEvent;
import com.ecom.shared.messaging.DomainEventRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InboxDeduplicationAspectTest {

   private InboxRepository repository;
   private PlatformTransactionManager transactionManager;
   private DomainEventRegistry eventRegistry;
   private InboxProperties properties;
   private SampleUseCase target;
   private InboxUseCase<Order.Placed> proxy;

   sealed interface Order extends DomainEvent {
      record Placed(String orderId) implements Order {
         @Override
         public String aggregateId() {
            return orderId;
         }
      }
   }

   static class SampleUseCase implements InboxUseCase<Order.Placed> {
      final AtomicInteger executionCount = new AtomicInteger(0);
      RuntimeException failure;

      @Override
      public Class<Order.Placed> subscribe() {
         return Order.Placed.class;
      }

      @Override
      public Void execute(InboxEvent<Order.Placed> cmd) {
         executionCount.incrementAndGet();
         if (failure != null) {
            throw failure;
         }
         return null;
      }
   }

   @BeforeEach
   void setUp() {
      repository = mock(InboxRepository.class);
      when(repository.saveAndFlush(any(InboxEntity.class))).thenAnswer(i -> i.getArgument(0));

      transactionManager = mock(PlatformTransactionManager.class);
      eventRegistry = new DomainEventRegistry();
      properties = new InboxProperties(86400, new InboxProperties.Cleaner(true, "0 0 3 * * ?", 500));

      target = new SampleUseCase();

      InboxDeduplicationAspect aspect = new InboxDeduplicationAspect(
            repository,
            eventRegistry,
            new TransactionTemplate(transactionManager),
            properties
      );

      AspectJProxyFactory factory = new AspectJProxyFactory(target);
      factory.addAspect(aspect);
      proxy = factory.getProxy();
   }

   @Test
   @DisplayName("Executes use case and saves inbox entity on first delivery")
   void executesAndSavesOnFirstDelivery() {
      InboxEvent<Order.Placed> event = new InboxEvent<>("msg-1", new Order.Placed("ord-100"));

      Void result = proxy.execute(event);

      assertNull(result);
      assertEquals(1, target.executionCount.get());

      ArgumentCaptor<InboxEntity> captor = ArgumentCaptor.forClass(InboxEntity.class);
      verify(repository).saveAndFlush(captor.capture());
      InboxEntity entity = captor.getValue();

      assertEquals("msg-1", entity.getMessageId());
      assertEquals("order.placed", entity.getSignature());
      assertNotNull(entity.getReceivedAt());
      assertNotNull(entity.getExpiresAt());
   }

   @Test
   @DisplayName("Skips execution when messageId is already in inbox repository")
   void skipsDuplicateMessage() {
      when(repository.existsByMessageId("msg-dup")).thenReturn(true);

      InboxEvent<Order.Placed> event = new InboxEvent<>("msg-dup", new Order.Placed("ord-100"));

      Void result = proxy.execute(event);

      assertNull(result);
      assertEquals(0, target.executionCount.get());
      verify(repository, never()).saveAndFlush(any());
   }

   @Test
   @DisplayName("A concurrent duplicate is dropped at the insert, before the business logic runs")
   void suppressesConcurrentDuplicateWithoutRunningBusinessLogic() {
      when(repository.saveAndFlush(any(InboxEntity.class))).thenThrow(new DataIntegrityViolationException("duplicate key"));

      InboxEvent<Order.Placed> event = new InboxEvent<>("msg-concurrent", new Order.Placed("ord-100"));

      Void result = proxy.execute(event);

      assertNull(result);
      assertEquals(0, target.executionCount.get());
      verify(transactionManager).rollback(any());
      verify(transactionManager, never()).commit(any());
   }

   @Test
   @DisplayName("A constraint violation raised by the business logic is not mistaken for a duplicate")
   void businessDataIntegrityViolationPropagates() {
      target.failure = new DataIntegrityViolationException("business unique constraint");

      InboxEvent<Order.Placed> event = new InboxEvent<>("msg-business", new Order.Placed("ord-100"));

      assertThrows(DataIntegrityViolationException.class, () -> proxy.execute(event));
      verify(transactionManager).rollback(any());
   }
}
