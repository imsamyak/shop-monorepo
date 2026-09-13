package com.ecom.outbox.internal.aspect;

import java.util.UUID;

/**
 * Spring application event published within the business transaction after an OutboxEntity is persisted.
 * Listened to via {@code @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)} to trigger
 * immediate async relaying.
 */
public record OutboxCreatedEvent(UUID outboxId) {
}
