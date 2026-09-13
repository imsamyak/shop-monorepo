package com.ecom.outbox;

/**
 * Processor contract for claiming, leasing, publishing, and updating outbox records.
 */
public interface OutboxProcessor {

   /**
     * Claims and processes a batch of outbox events until the batch is empty.
     *
     * @return number of events processed
     */
   int processBatch();

   /**
     * Recovers in-flight events whose leases have expired back to PENDING.
     */
   void recoverStaleLeases();

   /**
     * Replays dead-lettered events back to PENDING status.
     *
     * @return number of events replayed
     */
   int replayDeadLetterEvents();
}
