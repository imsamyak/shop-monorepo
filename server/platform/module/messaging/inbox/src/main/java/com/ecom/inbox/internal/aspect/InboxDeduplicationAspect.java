package com.ecom.inbox.internal.aspect;

import com.ecom.inbox.InboxEvent;
import com.ecom.inbox.internal.config.InboxProperties;
import com.ecom.inbox.internal.persistence.InboxEntity;
import com.ecom.inbox.internal.persistence.InboxRepository;
import com.ecom.shared.messaging.DomainEvent;
import com.ecom.shared.messaging.DomainEventRegistry;
import com.ecom.shared.messaging.DomainEventSignature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.UndeclaredThrowableException;
import java.time.Instant;

/**
 * AOP Aspect intercepting every {@code InboxUseCase#execute} invocation.
 * <p>
 * The inbox row is inserted (and flushed) FIRST, inside the same transaction as the business logic. A concurrent or
 * repeated delivery of the same message id violates the unique constraint at that insert, before any business code
 * ran, so the duplicate is dropped cleanly. A {@link DataIntegrityViolationException} thrown by the business logic
 * itself is NOT treated as a duplicate and propagates (so the broker retries / dead-letters the message).
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class InboxDeduplicationAspect {

   private final InboxRepository inboxRepository;
   private final DomainEventRegistry eventRegistry;
   private final TransactionTemplate transactionTemplate;
   private final InboxProperties properties;

   @Around("execution(* com.ecom.inbox.InboxUseCase+.execute(..))")
   public Object interceptUseCase(ProceedingJoinPoint joinPoint) throws Throwable {
      Object[] args = joinPoint.getArgs();
      if (args == null || args.length == 0 || !(args[0] instanceof InboxEvent<?> inboxEvent)) {
         return joinPoint.proceed();
      }

      String messageId = inboxEvent.messageId();

      if (inboxRepository.existsByMessageId(messageId)) {
         log.warn("Duplicate messageId={} detected in inbox. Skipping duplicate execution.", messageId);
         return null;
      }

      try {
         return transactionTemplate.execute(status -> {
            recordInboxMessage(inboxEvent);
            try {
               return joinPoint.proceed();
            } catch (RuntimeException | Error e) {
               throw e;
            } catch (Throwable t) {
               throw new UndeclaredThrowableException(t);
            }
         });
      } catch (DuplicateMessageException e) {
         log.warn("Concurrent duplicate messageId={} detected. Skipping duplicate execution.", messageId);
         return null;
      } catch (UndeclaredThrowableException e) {
         throw e.getCause();
      }
   }

   private void recordInboxMessage(InboxEvent<?> inboxEvent) {
      String messageId = inboxEvent.messageId();
      DomainEvent event = inboxEvent.event();

      DomainEventSignature signature = eventRegistry.getSignature(event);
      Instant now = Instant.now();

      InboxEntity entity = InboxEntity.builder()
            .messageId(messageId)
            .signature(signature.toString())
            .receivedAt(now)
            .expiresAt(now.plusSeconds(properties.deduplicationTtlSeconds()))
            .build();

      try {
         inboxRepository.saveAndFlush(entity);
      } catch (DataIntegrityViolationException e) {
         throw new DuplicateMessageException(messageId, e);
      }
   }

   /** Raised (inside the transaction, so it rolls back) when the message id is already recorded. */
   static class DuplicateMessageException extends RuntimeException {
      DuplicateMessageException(String messageId, Throwable cause) {
         super("Duplicate inbox message " + messageId, cause);
      }
   }
}
