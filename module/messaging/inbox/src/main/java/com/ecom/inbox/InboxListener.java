package com.ecom.inbox;

import java.util.Set;

/**
 * Ingestion listener through which services route raw broker messages into the transactional inbox.
 */
public interface InboxListener {

   /**
     * Ingests, deduplicates, and dispatches an incoming broker event to the matching {@link InboxUseCase}.
     *
     * @param messageId   unique identifier for the broker message (used for idempotency deduplication)
     * @param signature   canonical dot-delimited event signature (e.g., "order.placed")
     * @param payloadJson raw JSON payload of the domain event
     */
   void listen(String messageId, String signature, String payloadJson);

   /**
     * Aggregates (e.g. "product", "sku") that at least one registered use case subscribes to. Transports use this to
     * decide which topics to consume.
     */
   default Set<String> subscribedAggregates() {
      return Set.of();
   }
}
