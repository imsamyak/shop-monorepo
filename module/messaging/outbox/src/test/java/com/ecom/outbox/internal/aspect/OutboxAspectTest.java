package com.ecom.outbox.internal.aspect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ecom.outbox.OutboxEventFactory;
import com.ecom.outbox.OutboxUseCase;
import com.ecom.outbox.internal.persistence.OutboxEntity;
import com.ecom.outbox.internal.persistence.OutboxRepository;
import com.ecom.outbox.internal.persistence.OutboxStatus;
import com.ecom.shared.dto.result.Result;
import com.ecom.shared.messaging.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OutboxAspectTest {

   record SampleResult(String text) implements Result.One {}

   private final ObjectMapper mapper = new ObjectMapper();
   private OutboxRepository repository;
   private PlatformTransactionManager transactionManager;
   private SampleUseCase target;
   private OutboxUseCase<String, SampleResult> proxy;

   private OutboxUseCase<String, SampleResult> proxyWithLimit(int maxPayloadLength) {
      OutboxEventFactory eventFactory = new DefaultOutboxEventFactory(mapper, maxPayloadLength);
      OutboxAspect aspect = new OutboxAspect(repository, eventFactory,
            new TransactionTemplate(transactionManager));
      AspectJProxyFactory factory = new AspectJProxyFactory(target);
      factory.addAspect(aspect);
      return factory.getProxy();
   }

   @BeforeEach
   void setUp() {
      repository = mock(OutboxRepository.class);
      when(repository.save(any(OutboxEntity.class))).thenAnswer(i -> i.getArgument(0));
      transactionManager = mock(PlatformTransactionManager.class);
      target = new SampleUseCase();
      proxy = proxyWithLimit(10_000);
   }

   private OutboxEntity savedRow() {
      ArgumentCaptor<OutboxEntity> captor = ArgumentCaptor.forClass(OutboxEntity.class);
      verify(repository).save(captor.capture());
      return captor.getValue();
   }

   @Test
   void returnsTheUseCaseResultUnchanged() {
      assertEquals("result:x", proxy.execute("x").text());
   }

   @Test
   void savesOneRowWithAggregateTypeIdAndEnvelopePayload() throws IOException {
      UUID id = UUID.randomUUID();
      target.eventSupplier = () -> new Thing.Made(id, "widget");

      proxy.execute("x");

      OutboxEntity row = savedRow();
      assertEquals("thing", row.getAggregateType());
      assertEquals(id.toString(), row.getAggregateId());
      assertEquals(OutboxStatus.PENDING, row.getStatus());
      JsonNode json = mapper.readTree(row.getPayload());
      assertEquals("thing", json.get("aggregate").asText());
      assertEquals("made", json.get("action").asText());
      assertEquals("widget", json.get("data").get("name").asText());
      assertEquals(id.toString(), json.get("data").get("thingId").asText());
      assertEquals(3, json.size());
   }

   @Test
   void savesNothingWhenTheUseCaseReturnsNoEvent() {
      target.eventSupplier = () -> null;

      proxy.execute("x");

      verify(repository, never()).save(any());
   }

   @Test
   void failsTheUseCaseWhenThePayloadIsOverTheConfiguredLimit() {
      proxy = proxyWithLimit(20);
      target.eventSupplier = () -> new Thing.Made(UUID.randomUUID(), "widget");

      IllegalStateException ex = assertThrows(IllegalStateException.class, () -> proxy.execute("x"));
      assertTrue(ex.getMessage().contains("outbox.max-payload-length"), ex.getMessage());
      verify(repository, never()).save(any());
   }

   @Test
   void acceptsAPayloadExactlyAtTheLimit() throws IOException {
      UUID id = UUID.randomUUID();
      target.eventSupplier = () -> new Thing.Made(id, "widget");
      proxy.execute("x");
      int exact = savedRow().getPayload().length();

      repository = mock(OutboxRepository.class);
      when(repository.save(any(OutboxEntity.class))).thenAnswer(i -> i.getArgument(0));
      proxy = proxyWithLimit(exact);
      proxy.execute("x");
      assertEquals(exact, savedRow().getPayload().length());
   }

   @Test
   void anInvalidEventFailsTheUseCaseAndSavesNothing() {
      target.eventSupplier = Thing.Blank::new;

      assertThrows(IllegalArgumentException.class, () -> proxy.execute("x"));
      verify(repository, never()).save(any());
   }

   @Test
   void aRuntimeExceptionFromTheUseCaseSavesNothingRollsBackAndPropagates() {
      target.failure = new IllegalStateException("boom");
      target.eventSupplier = () -> new Thing.Made(UUID.randomUUID(), "widget");

      IllegalStateException ex = assertThrows(IllegalStateException.class, () -> proxy.execute("x"));
      assertEquals("boom", ex.getMessage());
      verify(repository, never()).save(any());
      verify(transactionManager).rollback(any());
   }

   @Test
   void aCheckedExceptionFromTheUseCaseIsNotSwallowedAndRollsBack() {
      target.checkedFailure = new IOException("disk");

      Throwable thrown = assertThrows(Throwable.class, () -> proxy.execute("x"));
      Throwable root = thrown instanceof UndeclaredThrowableException u ? u.getUndeclaredThrowable() : thrown;
      assertInstanceOf(IOException.class, root);

      verify(repository, never()).save(any());
      verify(transactionManager).rollback(any());
   }

   @Test
   void commitsTheTransactionWhenEverythingSucceeds() {
      target.eventSupplier = () -> new Thing.Made(UUID.randomUUID(), "widget");

      proxy.execute("x");

      verify(transactionManager).commit(any());
      verify(transactionManager, never()).rollback(any());
   }

   sealed interface Thing extends DomainEvent {
      record Made(UUID thingId, String name) implements Thing {
         @Override
         public String aggregateId() {
            return thingId.toString();
         }
      }

      record Blank() implements Thing {
         @Override
         public String aggregateId() {
            return "   ";
         }
      }
   }

   static class SampleUseCase implements OutboxUseCase<String, SampleResult> {
      Supplier<DomainEvent> eventSupplier = () -> null;
      RuntimeException failure;
      Exception checkedFailure;

      @SuppressWarnings("unchecked")
      private static <T extends Throwable> void sneaky(Throwable t) throws T {
         throw (T) t;
      }

      @Override
      public SampleResult execute(String command) {
         if (failure != null) {
            throw failure;
         }
         if (checkedFailure != null) {
            SampleUseCase.<RuntimeException>sneaky(checkedFailure);
         }
         return new SampleResult("result:" + command);
      }

      @Override
      public DomainEvent buildEvent(String command, SampleResult result) {
         return eventSupplier.get();
      }
   }
}
