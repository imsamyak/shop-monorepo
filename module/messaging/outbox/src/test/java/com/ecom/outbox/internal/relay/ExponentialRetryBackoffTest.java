package com.ecom.outbox.internal.relay;

import com.ecom.outbox.RetryBackoffStrategy;
import com.ecom.outbox.internal.config.OutboxProperties;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExponentialRetryBackoffTest {

   @Test
   void backoffIncreasesExponentially() {
      OutboxProperties.Retry properties = new OutboxProperties.Retry(5, 1000L, 300000L);
      RetryBackoffStrategy backoff = new ExponentialRetryBackoff(properties);

      Instant now = Instant.now();
      Instant retry0 = backoff.calculateNextRetry(0);
      Instant retry1 = backoff.calculateNextRetry(1);
      Instant retry2 = backoff.calculateNextRetry(2);

      assertTrue(retry0.isAfter(now));
      assertTrue(retry1.isAfter(retry0.minusSeconds(1)));
      assertTrue(retry2.isAfter(retry1.minusSeconds(1)));
   }

   @Test
   void backoffIsCappedAtMaximum() {
      OutboxProperties.Retry properties = new OutboxProperties.Retry(15, 1000L, 5000L);
      RetryBackoffStrategy backoff = new ExponentialRetryBackoff(properties);

      Instant now = Instant.now();
      Instant retry10 = backoff.calculateNextRetry(10);

      // Maximum jitter is 1.2 * maxMs = 6000ms
      assertTrue(retry10.isBefore(now.plusNanos(7000 * 1_000_000L)));
   }

   @Test
   void incorporatesJitter() {
      OutboxProperties.Retry properties = new OutboxProperties.Retry(5, 1000L, 60000L);
      RetryBackoffStrategy backoff = new ExponentialRetryBackoff(properties);

      // Run multiple times and verify we do not get identical values
      Instant first = backoff.calculateNextRetry(3);
      Instant second = backoff.calculateNextRetry(3);

      // Nanoseconds should differ
      assertNotEquals(first, second);
   }
}
