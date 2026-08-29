package com.ecom.outbox;

/**
 * Orchestrator interface for triggering outbox dispatching and polling.
 */
public interface OutboxRelay {

   /**
     * Polls the outbox table for pending events and dispatches them.
     */
   void poll();

   /**
     * Processes a batch of pending outbox events.
     *
     * @return total number of processed events
     */
   int processBatch();

   /**
     * Replays dead-lettered events back to PENDING status.
     *
     * @return number of events replayed
     */
   int replayDeadLetterEvents();
}
