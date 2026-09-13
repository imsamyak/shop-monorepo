package com.ecom.shared.messaging;

/**
 * Contract for events published across domain boundaries or transactional outbox.
 * The implementing record's own simple name is the action, and the declaring interface
 * is the aggregate type (e.g. Product.Created).
 */
public interface DomainEvent {

   /**
     * The identifier of the aggregate this event belongs to (converted to String).
     */
   String aggregateId();
}
