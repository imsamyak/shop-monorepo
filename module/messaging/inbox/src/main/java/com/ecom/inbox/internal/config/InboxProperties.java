package com.ecom.inbox.internal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Type-safe configuration properties for the inbox module.
 * Bound to prefix "inbox" in application.properties or application.yml.
 */
@ConfigurationProperties(prefix = "inbox")
public record InboxProperties(
      @DefaultValue("604800") long deduplicationTtlSeconds,
      @DefaultValue Cleaner cleaner
) {

   public record Cleaner(
         @DefaultValue("true") boolean enabled,
         @DefaultValue("0 0 3 * * ?") String cron,
         @DefaultValue("500") int batchSize
   ) {
   }
}
