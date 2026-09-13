package com.ecom.outbox;

import java.time.Instant;

/**
 * Strategy interface for calculating the next retry timestamp for a failed outbox event.
 * Pluggable via Spring Bean registration (defaults to exponential backoff with jitter).
 */
@FunctionalInterface
public interface RetryBackoffStrategy {

   /**
     * Calculates the timestamp when the next retry attempt should be eligible for processing.
     *
     * @param retryCount the 1-based attempt number that just failed
     * @return the timestamp when the next retry should take place
     */
   Instant calculateNextRetry(int retryCount);
}
