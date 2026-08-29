package com.ecom.outbox;

import com.ecom.outbox.internal.aspect.OutboxCreatedEvent;

/**
 * Asynchronous event trigger contract invoked after transaction commit.
 */
public interface OutboxEventTrigger {

   /**
     * Handles the outbox record creation after the transaction commits.
     *
     * @param event the outbox created event
     */
   void onOutboxCreated(OutboxCreatedEvent event);
}
