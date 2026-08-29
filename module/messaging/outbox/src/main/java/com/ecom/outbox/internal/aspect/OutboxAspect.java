package com.ecom.outbox.internal.aspect;

import com.ecom.outbox.OutboxEventFactory;
import com.ecom.outbox.OutboxUseCase;
import com.ecom.outbox.internal.persistence.OutboxEntity;
import com.ecom.outbox.internal.persistence.OutboxEvent;
import com.ecom.outbox.internal.persistence.OutboxRepository;
import com.ecom.shared.dto.result.CommandResult;
import com.ecom.shared.messaging.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;

/**
 * Wraps every {@link OutboxUseCase#execute} in a transaction and writes the outbox rows in that same
 * transaction, so the business change and its events commit or roll back together. If the caller already
 * has an active transaction, the aspect joins it.
 */
@RequiredArgsConstructor
@Slf4j
@Aspect
public class OutboxAspect {

   private final OutboxRepository outboxRepository;
   private final OutboxEventFactory outboxEventFactory;
   private final TransactionTemplate transactionTemplate;
   private final ApplicationEventPublisher eventPublisher;

   public OutboxAspect(OutboxRepository outboxRepository,
                  OutboxEventFactory outboxEventFactory,
                  TransactionTemplate transactionTemplate) {
      this(outboxRepository, outboxEventFactory, transactionTemplate, null);
   }

   @Around("execution(* com.ecom.outbox.OutboxUseCase+.execute(..))")
   public Object interceptUseCase(ProceedingJoinPoint joinPoint) throws Throwable {
      try {
         return transactionTemplate.execute(status -> {
            try {
               return proceedAndRecord(joinPoint);
            } catch (RuntimeException | Error e) {
               throw e;
            } catch (Throwable t) {
               throw new UndeclaredThrowableException(t);
            }
         });
      } catch (UndeclaredThrowableException e) {
         throw e.getCause();
      }
   }

   @SuppressWarnings("unchecked")
   private Object proceedAndRecord(ProceedingJoinPoint joinPoint) throws Throwable {
      Object result = joinPoint.proceed();

      OutboxUseCase<Object, CommandResult> useCase = (OutboxUseCase<Object, CommandResult>) joinPoint.getTarget();
      Object[] args = joinPoint.getArgs();
      Object command = (args != null && args.length > 0) ? args[0] : null;

      List<DomainEvent> domainEvents = useCase.buildEvents(command, (CommandResult) result);
      if (domainEvents == null || domainEvents.isEmpty()) {
         return result;
      }

      for (DomainEvent domainEvent : domainEvents) {
         if (domainEvent == null) {
            continue;
         }
         OutboxEvent outboxEvent = outboxEventFactory.create(domainEvent);
         OutboxEntity saved = outboxRepository.save(OutboxEntity.builder().event(outboxEvent).build());

         if (eventPublisher != null) {
            eventPublisher.publishEvent(new OutboxCreatedEvent(saved.getId()));
         }
      }

      return result;
   }
}
