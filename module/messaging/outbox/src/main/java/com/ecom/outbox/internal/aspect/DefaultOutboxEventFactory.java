package com.ecom.outbox.internal.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ecom.outbox.OutboxEventFactory;
import com.ecom.outbox.internal.persistence.OutboxEvent;
import com.ecom.shared.messaging.DomainEvent;
import com.ecom.shared.messaging.DomainEventRegistry;
import com.ecom.shared.messaging.DomainEventSignature;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

/**
 * Default implementation of {@link OutboxEventFactory} responsible for transforming raw
 * {@link DomainEvent} instances into validated, JSON-serialized {@link OutboxEvent} embeddable value objects.
 */
@RequiredArgsConstructor
public class DefaultOutboxEventFactory implements OutboxEventFactory {

   private final ObjectMapper objectMapper;
   private final DomainEventRegistry eventRegistry;
   private final int maxPayloadLength;

   public DefaultOutboxEventFactory(ObjectMapper objectMapper, int maxPayloadLength) {
      this(objectMapper, new DomainEventRegistry(), maxPayloadLength);
   }

   /**
   * Builds and validates an {@link OutboxEvent} from a domain event.
   */
   @Override
   public OutboxEvent create(DomainEvent domainEvent) {
      Objects.requireNonNull(domainEvent, "DomainEvent must not be null");

      DomainEventSignature signature = eventRegistry.getSignature(domainEvent);
      String aggregateType = signature.aggregate();
      String action = signature.action();

      String aggregateId = extractAggregateId(domainEvent, aggregateType, action);
      String jsonPayload = serializeAndValidate(signature, domainEvent);

      return OutboxEvent.builder()
          .aggregateType(aggregateType)
          .aggregateId(aggregateId)
          .payload(jsonPayload)
          .build();
   }

   private String extractAggregateId(DomainEvent event, String aggregateType, String action) {
      String id = event.aggregateId();
      if (id == null || id.isBlank()) {
         throw new IllegalArgumentException(aggregateType + "." + action + ".aggregateId() must not be null or blank");
      }
      return id;
   }

   private String serializeAndValidate(DomainEventSignature signature, DomainEvent event) {
      try {
         Envelope envelope = Envelope
             .builder()
             .aggregate(signature.aggregate())
             .action(signature.action())
             .data(event)
             .build();

         String json = objectMapper.writeValueAsString(envelope);

         if (json.length() > maxPayloadLength) {
         throw new IllegalStateException("Outbox payload for " + signature
            + " is " + json.length() + " characters, exceeds outbox.max-payload-length (" + maxPayloadLength + ")");
         }

         return json;
      } catch (JsonProcessingException e) {
         throw new IllegalArgumentException("Failed to serialize outbox event payload for " + signature, e);
      }
   }

   /**
   * Internal serialization envelope: { aggregate, action, data }.
   */
   @Builder
   public record Envelope(String aggregate, String action, Object data) {
   }
}
