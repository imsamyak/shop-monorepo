package com.ecom.outbox.internal.relay;

import com.ecom.outbox.OutboxCleaner;
import com.ecom.outbox.OutboxProcessor;
import com.ecom.outbox.OutboxRelay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

   @Mock
   private OutboxProcessor processor;

   @Mock
   private OutboxCleaner cleaner;

   private OutboxRelay relay;

   @BeforeEach
   void setUp() {
      relay = new DefaultOutboxRelay(processor, cleaner);
   }

   @Test
   void shouldDelegateProcessBatchWithoutRunningTheCleaner() {
      when(processor.processBatch()).thenReturn(5);

      int count = relay.processBatch();

      assertEquals(5, count);
      verify(processor).processBatch();
      // retention cleanup runs on its own cron schedule, not after every batch
      verify(cleaner, never()).cleanup();
   }

   @Test
   void shouldNotTriggerCleanerWhenNoEventsProcessed() {
      when(processor.processBatch()).thenReturn(0);

      int count = relay.processBatch();

      assertEquals(0, count);
      verify(processor).processBatch();
      verify(cleaner, never()).cleanup();
   }

   @Test
   void shouldSkipProcessingIfAlreadyInProgress() throws Exception {
      Field isProcessingField = DefaultOutboxRelay.class.getDeclaredField("isProcessing");
      isProcessingField.setAccessible(true);
      AtomicBoolean isProcessing = (AtomicBoolean) isProcessingField.get(relay);
      isProcessing.set(true);

      int count = relay.processBatch();

      assertEquals(0, count);
      verify(processor, never()).processBatch();
   }

   @Test
   void pollShouldInvokeProcessBatch() {
      when(processor.processBatch()).thenReturn(2);

      relay.poll();

      verify(processor).processBatch();
   }

   @Test
   void shouldDelegateReplayDeadLetterEventsToProcessor() {
      when(processor.replayDeadLetterEvents()).thenReturn(3);

      int replayed = relay.replayDeadLetterEvents();

      assertEquals(3, replayed);
      verify(processor).replayDeadLetterEvents();
   }
}
