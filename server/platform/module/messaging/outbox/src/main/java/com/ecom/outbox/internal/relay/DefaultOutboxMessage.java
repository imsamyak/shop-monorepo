package com.ecom.outbox.internal.relay;

import com.ecom.outbox.OutboxMessage;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * Default immutable record implementation of {@link OutboxMessage}.
 * Encapsulated within the internal relay engine.
 */
@Builder
public record DefaultOutboxMessage(
      UUID id,
      String aggregateType,
      String aggregateId,
      String payload,
      Instant createdAt
) implements OutboxMessage {
}
