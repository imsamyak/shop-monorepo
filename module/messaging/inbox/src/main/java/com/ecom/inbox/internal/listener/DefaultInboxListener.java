package com.ecom.inbox.internal.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ecom.inbox.InboxEvent;
import com.ecom.inbox.InboxListener;
import com.ecom.inbox.InboxUseCase;
import com.ecom.shared.messaging.DomainEvent;
import com.ecom.shared.messaging.DomainEventRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link InboxListener}.
 * Automatically registers subscribed domain event types into {@link DomainEventRegistry}
 * and routes incoming broker messages to the corresponding {@link InboxUseCase}.
 */
@Slf4j
public class DefaultInboxListener implements InboxListener {

   private final DomainEventRegistry eventRegistry;
   private final ObjectMapper objectMapper;
   private final Map<Class<? extends DomainEvent>, InboxUseCase<DomainEvent>> handlers = new ConcurrentHashMap<>();

   public DefaultInboxListener(DomainEventRegistry eventRegistry,
                        ObjectMapper objectMapper,
                        ObjectProvider<InboxUseCase<?>> useCaseProvider) {
      this.eventRegistry = Objects.requireNonNull(eventRegistry, "DomainEventRegistry must not be null");
      this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper must not be null");

      if (useCaseProvider != null) {
         useCaseProvider.orderedStream().forEach(this::registerUseCase);
      }
   }

   public DefaultInboxListener(DomainEventRegistry eventRegistry, ObjectMapper objectMapper) {
      this(eventRegistry, objectMapper, null);
   }

   @SuppressWarnings("unchecked")
   public <E extends DomainEvent> void registerUseCase(InboxUseCase<E> useCase) {
      Objects.requireNonNull(useCase, "InboxUseCase must not be null");
      Class<E> eventClass = useCase.subscribe();
      Objects.requireNonNull(eventClass, "InboxUseCase.subscribe() must not return null");

      eventRegistry.register(eventClass);
      handlers.put(eventClass, (InboxUseCase<DomainEvent>) useCase);
      log.info("Registered InboxUseCase for event: {} ({})", eventClass.getSimpleName(), eventRegistry.getSignature(eventClass));
   }

   @Override
   public Set<String> subscribedAggregates() {
      Set<String> aggregates = new TreeSet<>();
      for (Class<? extends DomainEvent> eventClass : handlers.keySet()) {
         aggregates.add(eventRegistry.getSignature(eventClass).aggregate());
      }
      return aggregates;
   }

   @Override
   public void listen(String messageId, String signature, String payloadJson) {
      Objects.requireNonNull(messageId, "messageId must not be null");
      Objects.requireNonNull(signature, "signature must not be null");
      Objects.requireNonNull(payloadJson, "payloadJson must not be null");

      Class<? extends DomainEvent> eventClass = eventRegistry.getClazz(signature);
      if (eventClass == null) {
         log.debug("No domain event registered for signature: {}. Skipping message {}.", signature, messageId);
         return;
      }

      InboxUseCase<DomainEvent> useCase = handlers.get(eventClass);
      if (useCase == null) {
         log.debug("No InboxUseCase registered for class: {}. Skipping message {}.", eventClass.getName(), messageId);
         return;
      }

      DomainEvent event = deserialize(payloadJson, eventClass, signature);
      InboxEvent<DomainEvent> inboxEvent = new InboxEvent<>(messageId, event);

      useCase.execute(inboxEvent);
   }

   private DomainEvent deserialize(String payloadJson, Class<? extends DomainEvent> eventClass, String signature) {
      try {
         return objectMapper.readValue(payloadJson, eventClass);
      } catch (JsonProcessingException e) {
         throw new IllegalArgumentException("Failed to deserialize inbox payload for signature: " + signature, e);
      }
   }
}
