package com.ecom.outbox.internal.relay;

import com.ecom.outbox.OutboxCleaner;
import com.ecom.outbox.OutboxProcessor;
import com.ecom.outbox.OutboxRelay;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default implementation of {@link OutboxRelay}.
 * Orchestrates when to run (triggered by poller or event) and delegates to {@link OutboxProcessor}.
 * Retention cleanup is scheduled separately (see {@code outbox.cleaner.cron}).
 */
@RequiredArgsConstructor
@Slf4j
public class DefaultOutboxRelay implements OutboxRelay {

   private final OutboxProcessor processor;
   private final OutboxCleaner cleaner;
   private final AtomicBoolean isProcessing = new AtomicBoolean(false);

   @Override
   public void poll() {
      processBatch();
   }

   @Override
   public int processBatch() {
      if (!isProcessing.compareAndSet(false, true)) {
         return 0;
      }

      try {
         return processor.processBatch();
      } finally {
         isProcessing.set(false);
      }
   }

   @Override
   public int replayDeadLetterEvents() {
      return processor.replayDeadLetterEvents();
   }
}
