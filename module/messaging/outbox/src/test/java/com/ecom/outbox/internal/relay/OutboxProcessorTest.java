package com.ecom.outbox.internal.relay;

import com.ecom.outbox.OutboxMessage;
import com.ecom.outbox.OutboxProcessor;
import com.ecom.outbox.OutboxPublisher;
import com.ecom.outbox.RetryBackoffStrategy;
import com.ecom.outbox.internal.config.OutboxProperties;
import com.ecom.outbox.internal.persistence.OutboxEntity;
import com.ecom.outbox.internal.persistence.OutboxEvent;
import com.ecom.outbox.internal.persistence.OutboxRepository;
import com.ecom.outbox.internal.persistence.OutboxStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxProcessorTest {

   @Mock
   private OutboxRepository repository;

   @Mock
   private OutboxPublisher publisher;

   @Mock
   private TransactionTemplate transactionTemplate;

   private ObjectProvider<OutboxPublisher> publisherProvider;
   private OutboxProcessor processor;

   @BeforeEach
   @SuppressWarnings("unchecked")
   void setUp() {
      publisherProvider = mock(ObjectProvider.class);
      lenient().when(publisherProvider.getIfAvailable()).thenReturn(publisher);

      lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
         org.springframework.transaction.support.TransactionCallback<?> callback = invocation.getArgument(0);
         return callback.doInTransaction(null);
      });
      lenient().doAnswer(invocation -> {
         java.util.function.Consumer<?> consumer = invocation.getArgument(0);
         consumer.accept(null);
         return null;
      }).when(transactionTemplate).executeWithoutResult(any());

      OutboxProperties properties = new OutboxProperties(
            65536,
            5000L,
            new OutboxProperties.Relay(true, 10, 30),
            new OutboxProperties.Cleaner(7, "0 0 2 * * ?"),
            new OutboxProperties.Retry(3, 1000L, 300000L)
      );

      RetryBackoffStrategy backoffStrategy = new ExponentialRetryBackoff(properties.retry());
      processor = new DefaultOutboxProcessor(repository, publisherProvider, transactionTemplate, backoffStrategy, properties);
   }

   @Test
   void shouldPublishEventAndMarkProcessed() throws Exception {
      UUID id = UUID.randomUUID();
      OutboxEvent event = OutboxEvent.builder()
            .aggregateType("Product")
            .aggregateId("123")
            .payload("{\"title\":\"Phone\"}")
            .build();
      OutboxEntity entity = OutboxEntity.builder()
            .id(id)
            .event(event)
            .status(OutboxStatus.PENDING)
            .build();

      when(repository.lockPendingBatch(eq(OutboxStatus.PENDING), any(Instant.class), any(Pageable.class)))
            .thenReturn(List.of(entity))
            .thenReturn(List.of());

      when(repository.save(any(OutboxEntity.class))).thenAnswer(i -> i.getArgument(0));
      when(repository.findById(id)).thenReturn(Optional.of(entity));

      int count = processor.processBatch();

      assertEquals(1, count);
      verify(publisher).publish(any(OutboxMessage.class));
      assertEquals(OutboxStatus.PROCESSED, entity.getStatus());
      assertNotNull(entity.getProcessedAt());
   }

   @Test
   void shouldMoveToDeadLetterWhenMaxRetriesExceeded() throws Exception {
      UUID id = UUID.randomUUID();
      OutboxEvent event = OutboxEvent.builder()
            .aggregateType("Product")
            .aggregateId("123")
            .payload("{\"title\":\"Corrupt\"}")
            .build();
      OutboxEntity entity = OutboxEntity.builder()
            .id(id)
            .event(event)
            .status(OutboxStatus.PENDING)
            .retryCount(2)
            .maxRetries(3)
            .build();

      when(repository.lockPendingBatch(eq(OutboxStatus.PENDING), any(Instant.class), any(Pageable.class)))
            .thenReturn(List.of(entity));

      when(repository.save(any(OutboxEntity.class))).thenAnswer(i -> i.getArgument(0));
      when(repository.findById(id)).thenReturn(Optional.of(entity));

      doThrow(new RuntimeException("Schema validation failed")).when(publisher).publish(any());

      processor.processBatch();

      assertEquals(OutboxStatus.DEAD_LETTER, entity.getStatus());
      assertEquals(3, entity.getRetryCount());
      assertTrue(entity.getLastError().contains("Schema validation failed"));
   }

   @Test
   void shouldReleaseStaleInFlightLeasesBeforeProcessing() {
      when(repository.lockPendingBatch(eq(OutboxStatus.PENDING), any(Instant.class), any(Pageable.class)))
            .thenReturn(List.of());

      processor.processBatch();

      verify(repository).releaseStaleInFlight(any(Instant.class));
   }

   @Test
   void shouldReplayDeadLetterEvents() {
      when(repository.replayDeadLetter()).thenReturn(5);

      int replayed = processor.replayDeadLetterEvents();

      assertEquals(5, replayed);
      verify(repository).replayDeadLetter();
   }
}
