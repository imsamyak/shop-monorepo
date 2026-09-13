package com.ecom.outbox;

import com.ecom.shared.dto.result.CommandResult;
import com.ecom.shared.messaging.DomainEvent;
import com.ecom.shared.usecase.CommandUseCase;

import java.util.List;

/**
 * A {@link CommandUseCase} that emits events through the transactional outbox.
 * The outbox rows are written in the same transaction as {@link #execute}.
 * Override {@link #buildEvent} for a single event, or {@link #buildEvents} when one command produces several
 * (for example deleting a product also deletes its SKUs). Returning {@code null} / an empty list skips emitting.
 */
public interface OutboxUseCase<C, R extends CommandResult> extends CommandUseCase<C, R> {

   /**
     * Builds the domain event from the command and the command result.
     */
   default DomainEvent buildEvent(C cmd, R result) {
      return null;
   }

   /**
     * Builds every domain event this command produces. Defaults to the single {@link #buildEvent} event.
     */
   default List<DomainEvent> buildEvents(C cmd, R result) {
      DomainEvent event = buildEvent(cmd, result);
      return event == null ? List.of() : List.of(event);
   }
}
