package com.ecom.outbox.internal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Type-safe configuration properties for the outbox module.
 * Bound to prefix "outbox" in application.properties or application.yml.
 */
@ConfigurationProperties(prefix = "outbox")
public record OutboxProperties(
      @DefaultValue("65536") int maxPayloadLength,
      @DefaultValue("5000") long pollIntervalMs,
      @DefaultValue Relay relay,
      @DefaultValue Cleaner cleaner,
      @DefaultValue Retry retry
) {

   public record Relay(
         @DefaultValue("true") boolean enabled,
         @DefaultValue("100") int batchSize,
         @DefaultValue("30") int leaseSeconds
   ) {
   }

   public record Cleaner(
         @DefaultValue("7") int retentionDays,
         @DefaultValue("0 0 2 * * ?") String cron
   ) {
   }

   public record Retry(
         @DefaultValue("5") int maxRetries,
         @DefaultValue("1000") long baseIntervalMs,
         @DefaultValue("300000") long maxIntervalMs
   ) {
   }
}
