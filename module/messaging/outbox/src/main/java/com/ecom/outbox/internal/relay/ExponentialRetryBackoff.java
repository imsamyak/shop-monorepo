package com.ecom.outbox.internal.relay;

import com.ecom.outbox.RetryBackoffStrategy;
import com.ecom.outbox.internal.config.OutboxProperties;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Default implementation of {@link RetryBackoffStrategy} using exponential backoff with +/-20% jitter.
 * Reads base interval and maximum cap directly from OutboxProperties.Retry.
 */
@RequiredArgsConstructor
public class ExponentialRetryBackoff implements RetryBackoffStrategy {

   private final OutboxProperties.Retry properties;
   private static final double JITTER_FACTOR = 0.2;

   @Override
   public Instant calculateNextRetry(int retryCount) {
      long baseIntervalMs = properties.baseIntervalMs();
      long maxIntervalMs = properties.maxIntervalMs();

      long exponential = baseIntervalMs * (1L << Math.min(retryCount, 30));
      long capped = Math.min(exponential, maxIntervalMs);

      double jitterRange = capped * JITTER_FACTOR;
      double minJitter = capped - jitterRange;
      double maxJitter = capped + jitterRange;
      long backoffWithJitter = (long) ThreadLocalRandom.current().nextDouble(minJitter, maxJitter + 1);

      return Instant.now().plusNanos(backoffWithJitter * 1_000_000L);
   }
}
