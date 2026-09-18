package com.ecom.outbox.internal.persistence;

/**
 * Status lifecycle states for outbox events.
 */
public enum OutboxStatus {
   PENDING,
   IN_FLIGHT,
   PROCESSED,
   DEAD_LETTER
}
