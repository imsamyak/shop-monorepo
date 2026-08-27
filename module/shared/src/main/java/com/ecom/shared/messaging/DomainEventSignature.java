package com.ecom.shared.messaging;

import java.util.Locale;

/**
 * Value object representing the canonical signature of a domain event,
 * consisting of its aggregate boundary and specific action normalized
 * to lower kebab-case (e.g., "product.created", "inventory.stock-added").
 */
public record DomainEventSignature(String aggregate, String action) {

   public DomainEventSignature(String aggregate, String action) {
      if (aggregate == null || aggregate.isBlank()) {
         throw new IllegalArgumentException("Aggregate must not be null or blank");
      }
      if (action == null || action.isBlank()) {
         throw new IllegalArgumentException("Action must not be null or blank");
      }
      this.aggregate = toKebabCase(aggregate);
      this.action = toKebabCase(action);
   }

   private static String toKebabCase(String input) {
      return input.trim()
            .replaceAll("([a-z0-9])([A-Z])", "$1-$2")
            .replaceAll("([A-Z]+)([A-Z][a-z0-9])", "$1-$2")
            .toLowerCase(Locale.ROOT);
   }

   @SuppressWarnings("NullableProblems")
   @Override
   public String toString() {
      return aggregate + "." + action;
   }
}
