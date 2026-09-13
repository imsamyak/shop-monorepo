package com.ecom.outbox.internal.relay;

import com.ecom.outbox.OutboxMessage;
import com.ecom.outbox.OutboxProcessor;
import com.ecom.outbox.OutboxPublisher;
import com.ecom.outbox.RetryBackoffStrategy;
import com.ecom.outbox.internal.config.OutboxProperties;
import com.ecom.outbox.internal.persistence.OutboxEntity;
import com.ecom.outbox.internal.persistence.OutboxRepository;
import com.ecom.outbox.internal.persistence.OutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Default implementation of {@link OutboxProcessor}.
 * Handles claiming, leasing, publishing, retrying, and updating event records.
 */
@RequiredArgsConstructor
@Slf4j
public class DefaultOutboxProcessor implements OutboxProcessor {

   private final OutboxRepository repository;
   private final ObjectProvider<OutboxPublisher> publisherProvider;
   private final TransactionTemplate transactionTemplate;
   private final RetryBackoffStrategy backoffStrategy;
   private final OutboxProperties properties;

   @Override
   public int processBatch() {
      if (publisherProvider.getIfAvailable() == null) {
         log.warn("No OutboxPublisher bean found; outbox events stay pending");
         return 0;
      }

      recoverStaleLeases();

      int batchSize = properties.relay().batchSize();
      int totalProcessed = 0;

      List<OutboxEntity> claimedBatch;
      boolean progressed;
      do {
         claimedBatch = claimNextBatch(batchSize);
         progressed = false;
         for (OutboxEntity entity : claimedBatch) {
            progressed |= processSingleEvent(entity);
            totalProcessed++;
         }
         // Keep going while events are being published: publishing an aggregate's event unblocks its successor.
      } while (!claimedBatch.isEmpty() && progressed);

      return totalProcessed;
   }

   @Override
   public void recoverStaleLeases() {
      try {
         transactionTemplate.executeWithoutResult(status -> {
            int recovered = repository.releaseStaleInFlight(Instant.now());
            if (recovered > 0) {
               log.warn("Recovered {} orphaned in-flight outbox events whose leases expired", recovered);
            }
         });
      } catch (Exception e) {
         log.error("Failed to recover stale in-flight leases: {}", e.getMessage(), e);
      }
   }

   private List<OutboxEntity> claimNextBatch(int batchSize) {
      return transactionTemplate.execute(status -> {
         Instant now = Instant.now();
         List<OutboxEntity> candidates = repository.lockPendingBatch(
               OutboxStatus.PENDING,
               now,
               PageRequest.of(0, batchSize, Sort.by("createdAt").ascending())
         );

         if (candidates.isEmpty()) {
            return List.of();
         }

         Instant leaseExpiration = now.plusSeconds(properties.relay().leaseSeconds());
         List<OutboxEntity> batch = new ArrayList<>(candidates.size());
         for (OutboxEntity entity : candidates) {
            entity.setStatus(OutboxStatus.IN_FLIGHT);
            entity.setLockedUntil(leaseExpiration);
            repository.save(entity);
            batch.add(entity);
         }
         return batch;
      });
   }

   private boolean processSingleEvent(OutboxEntity entity) {
      OutboxPublisher publisher = publisherProvider.getIfAvailable();
      if (publisher == null) {
         log.warn("No OutboxPublisher bean found. Reverting outbox event {} to PENDING", entity.getId());
         revertToPending(entity.getId());
         return false;
      }

      OutboxMessage message = DefaultOutboxMessage
            .builder()
            .id(entity.getId())
            .aggregateType(entity.getAggregateType())
            .aggregateId(entity.getAggregateId())
            .payload(entity.getPayload())
            .createdAt(entity.getCreatedAt())
            .build();

      try {
         publisher.publish(message);
         markProcessed(entity.getId());
         return true;
      } catch (Exception ex) {
         handleFailure(entity.getId(), ex);
         return false;
      }
   }

   private void markProcessed(UUID eventId) {
      try {
         transactionTemplate.executeWithoutResult(status -> {
            repository.findById(eventId).ifPresent(entity -> {
               entity.setStatus(OutboxStatus.PROCESSED);
               entity.setProcessedAt(Instant.now());
               entity.setLockedUntil(null);
               entity.setLastError(null);
               repository.save(entity);
            });
         });
      } catch (Exception e) {
         log.error("Failed to mark outbox event {} as PROCESSED: {}", eventId, e.getMessage(), e);
      }
   }

   private void handleFailure(UUID eventId, Exception ex) {
      try {
         transactionTemplate.executeWithoutResult(status -> {
            repository.findById(eventId).ifPresent(entity -> {
               int nextRetry = entity.getRetryCount() + 1;
               entity.setRetryCount(nextRetry);
               entity.setLockedUntil(null);

               String errorMsg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
               if (errorMsg.length() > 2000) {
                  errorMsg = errorMsg.substring(0, 2000);
               }
               entity.setLastError(errorMsg);

               int maxAllowed = entity.getMaxRetries() > 0 ? entity.getMaxRetries() : properties.retry().maxRetries();
               if (nextRetry >= maxAllowed) {
                  entity.setStatus(OutboxStatus.DEAD_LETTER);
                  entity.setNextRetryAt(null);
                  log.error("Outbox event {} reached max retries ({}); moved to DEAD_LETTER: {}",
                        eventId, maxAllowed, errorMsg);
               } else {
                  entity.setStatus(OutboxStatus.PENDING);
                  entity.setNextRetryAt(backoffStrategy.calculateNextRetry(nextRetry));
                  log.warn("Outbox event {} failed (attempt {}/{}). Next retry scheduled at {}: {}",
                        eventId, nextRetry, maxAllowed, entity.getNextRetryAt(), errorMsg);
               }
               repository.save(entity);
            });
         });
      } catch (Exception e) {
         log.error("Failed to record failure for outbox event {}: {}", eventId, e.getMessage(), e);
      }
   }

   private void revertToPending(UUID eventId) {
      try {
         transactionTemplate.executeWithoutResult(status -> {
            repository.findById(eventId).ifPresent(entity -> {
               entity.setStatus(OutboxStatus.PENDING);
               entity.setLockedUntil(null);
               repository.save(entity);
            });
         });
      } catch (Exception e) {
         log.error("Failed to revert event {} to PENDING: {}", eventId, e.getMessage());
      }
   }

   @Override
   public int replayDeadLetterEvents() {
      Integer count = transactionTemplate.execute(status -> repository.replayDeadLetter());
      int replayed = count != null ? count : 0;
      if (replayed > 0) {
         log.info("Operator replayed {} dead-letter outbox events back to PENDING", replayed);
      }
      return replayed;
   }
}
