package com.ecom.outbox;

import com.ecom.outbox.internal.persistence.OutboxEvent;
import com.ecom.shared.messaging.DomainEvent;

/**
 * Factory contract responsible for transforming raw {@link DomainEvent} instances into validated,
 * JSON-serialized {@link OutboxEvent} embeddable value objects.
 */
public interface OutboxEventFactory {

   /**
     * Builds and validates an {@link OutboxEvent} from a domain event.
     *
     * @param domainEvent the raw domain event
     * @return the serialized and validated outbox event value object
     */
   OutboxEvent create(DomainEvent domainEvent);
}
