package com.ecom.outbox;

/**
 * Periodically purges processed outbox events older than the configured retention threshold.
 */
public interface OutboxCleaner {

   /**
     * Executes the retention cleanup job.
     */
   void cleanup();
}
