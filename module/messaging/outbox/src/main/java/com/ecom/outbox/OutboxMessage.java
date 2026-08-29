package com.ecom.outbox;

import java.time.Instant;
import java.util.UUID;

/**
 * Public message interface dispatched to {@link OutboxPublisher}.
 * Defined at the root API level as an interface so external clients and SPI
 * implementors depend solely on pure contracts.
 */
public interface OutboxMessage {

   UUID id();

   String aggregateType();

   String aggregateId();

   String payload();

   Instant createdAt();
}
