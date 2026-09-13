package com.ecom.outbox.internal.relay;

import com.ecom.outbox.OutboxCleaner;
import com.ecom.outbox.internal.persistence.OutboxRepository;
import com.ecom.outbox.internal.persistence.OutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

/**
 * Default implementation of {@link OutboxCleaner}.
 * Periodically purges processed outbox events older than the configured retention threshold.
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultOutboxCleaner implements OutboxCleaner {

   private final OutboxRepository repository;
   private final TransactionTemplate transactionTemplate;
   private final int retentionDays;

   @Override
   public void cleanup() {
      Instant cutoff = Instant.now().minus(java.time.Duration.ofDays(retentionDays));
      try {
         Integer deleted = transactionTemplate.execute(status ->
               repository.deleteProcessedBefore(OutboxStatus.PROCESSED, cutoff)
         );
         if (deleted != null && deleted > 0) {
            log.info("OutboxCleaner removed {} processed events older than {}", deleted, cutoff);
         }
      } catch (Exception e) {
         log.warn("OutboxCleaner failed: {}", e.getMessage());
      }
   }
}
