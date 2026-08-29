package com.ecom.outbox.internal.relay;

import com.ecom.outbox.OutboxEventTrigger;
import com.ecom.outbox.OutboxRelay;
import com.ecom.outbox.internal.aspect.OutboxCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Default implementation of {@link OutboxEventTrigger}.
 * Listens to {@link OutboxCreatedEvent} after the business transaction commits,
 * triggering the relay worker asynchronously for near real-time message dispatch.
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultOutboxEventTrigger implements OutboxEventTrigger {

   private final OutboxRelay relay;
   private final TaskExecutor executor;

   @Override
   @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
   public void onOutboxCreated(OutboxCreatedEvent event) {
      executor.execute(() -> {
         try {
            relay.processBatch();
         } catch (Exception e) {
            log.warn("Asynchronous outbox trigger execution failed: {}", e.getMessage());
         }
      });
   }
}
