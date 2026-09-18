package com.ecom.outbox.internal.persistence;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for managing {@link OutboxEntity} instances with pessimistic locking.
 */
public interface OutboxRepository extends JpaRepository<OutboxEntity, UUID> {

   /**
     * Claims the next publishable rows. An event is only eligible when no older event of the same aggregate is
     * still unpublished (pending, in flight or dead-lettered), so events of one aggregate are published strictly in
     * order even across retries. Rows locked by another relay instance are skipped.
     */
   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
   @Query("""
      SELECT e FROM OutboxEntity e
      WHERE e.status = :status
          AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :now)
          AND NOT EXISTS (
              SELECT 1 FROM OutboxEntity older
              WHERE older.event.aggregateType = e.event.aggregateType
            AND older.event.aggregateId = e.event.aggregateId
            AND older.createdAt < e.createdAt
            AND older.status <> com.ecom.outbox.internal.persistence.OutboxStatus.PROCESSED
          )
      ORDER BY e.createdAt ASC
   """)
   List<OutboxEntity> lockPendingBatch(
         @Param("status") OutboxStatus status,
         @Param("now") Instant now,
         Pageable pageable
   );

   @Modifying
   @Query("""
      UPDATE OutboxEntity e
      SET e.status = com.ecom.outbox.internal.persistence.OutboxStatus.PENDING, e.lockedUntil = null
      WHERE e.status = com.ecom.outbox.internal.persistence.OutboxStatus.IN_FLIGHT AND e.lockedUntil < :now
   """)
   int releaseStaleInFlight(@Param("now") Instant now);

   @Modifying
   @Query("""
      UPDATE OutboxEntity e
      SET e.status = com.ecom.outbox.internal.persistence.OutboxStatus.PENDING,
         e.retryCount = 0, e.nextRetryAt = null, e.lastError = null
      WHERE e.status = com.ecom.outbox.internal.persistence.OutboxStatus.DEAD_LETTER
   """)
   int replayDeadLetter();

   @Modifying
   @Query("DELETE FROM OutboxEntity e WHERE e.id IN :ids")
   int deleteByIds(@Param("ids") List<UUID> ids);

   @Modifying
   @Query("DELETE FROM OutboxEntity e WHERE e.status = :status AND e.processedAt < :cutoff")
   int deleteProcessedBefore(@Param("status") OutboxStatus status, @Param("cutoff") Instant cutoff);
}
