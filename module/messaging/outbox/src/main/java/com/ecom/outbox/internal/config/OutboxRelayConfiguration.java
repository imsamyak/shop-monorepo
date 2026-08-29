package com.ecom.outbox.internal.config;

import com.ecom.outbox.OutboxCleaner;
import com.ecom.outbox.OutboxEventTrigger;
import com.ecom.outbox.OutboxProcessor;
import com.ecom.outbox.OutboxPublisher;
import com.ecom.outbox.OutboxRelay;
import com.ecom.outbox.RetryBackoffStrategy;
import com.ecom.outbox.internal.persistence.OutboxRepository;
import com.ecom.outbox.internal.relay.DefaultOutboxCleaner;
import com.ecom.outbox.internal.relay.DefaultOutboxEventTrigger;
import com.ecom.outbox.internal.relay.DefaultOutboxProcessor;
import com.ecom.outbox.internal.relay.DefaultOutboxRelay;
import com.ecom.outbox.internal.relay.ExponentialRetryBackoff;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;

/**
 * Configuration for the outbox relay background processor, trigger executor, cleaner, and scheduler.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelayConfiguration {

   @Bean("outboxTriggerExecutor")
   @ConditionalOnMissingBean(name = "outboxTriggerExecutor")
   public TaskExecutor getOutboxTriggerExecutor() {
      SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("outbox-trigger-");
      executor.setVirtualThreads(true);
      return executor;
   }

   @Bean("outboxCleaner")
   @ConditionalOnMissingBean
   public OutboxCleaner getOutboxCleaner(
         ObjectProvider<OutboxRepository> repositoryProvider,
         TransactionTemplate transactionTemplate,
         OutboxProperties properties
   ) {
      return new DefaultOutboxCleaner(
            repositoryProvider.getObject(),
            transactionTemplate,
            properties.cleaner().retentionDays()
      );
   }

   @Bean("retryBackoffStrategy")
   @ConditionalOnMissingBean
   public RetryBackoffStrategy getRetryBackoffStrategy(OutboxProperties properties) {
      return new ExponentialRetryBackoff(properties.retry());
   }

   @Bean("outboxProcessor")
   @ConditionalOnMissingBean
   public OutboxProcessor getOutboxProcessor(
         ObjectProvider<OutboxRepository> repositoryProvider,
         ObjectProvider<OutboxPublisher> publisherProvider,
         TransactionTemplate transactionTemplate,
         RetryBackoffStrategy backoffStrategy,
         OutboxProperties properties
   ) {
      return new DefaultOutboxProcessor(
            repositoryProvider.getObject(),
            publisherProvider,
            transactionTemplate,
            backoffStrategy,
            properties
      );
   }

   @Bean("outboxRelay")
   @ConditionalOnMissingBean
   public OutboxRelay getOutboxRelay(
         OutboxProcessor processor,
         OutboxCleaner cleaner
   ) {
      return new DefaultOutboxRelay(processor, cleaner);
   }

   @Bean("outboxEventTrigger")
   @ConditionalOnMissingBean
   public OutboxEventTrigger getOutboxEventTrigger(
         OutboxRelay relay,
         @Qualifier("outboxTriggerExecutor") TaskExecutor executor
   ) {
      return new DefaultOutboxEventTrigger(relay, executor);
   }

   @Bean("outboxSchedulingConfigurer")
   @ConditionalOnMissingBean(name = "outboxSchedulingConfigurer")
   public SchedulingConfigurer getOutboxSchedulingConfigurer(
         OutboxRelay relay,
         OutboxCleaner cleaner,
         OutboxProperties properties
   ) {
      return taskRegistrar -> {
         taskRegistrar.addFixedDelayTask(
               relay::poll,
               Duration.ofMillis(properties.pollIntervalMs())
         );
         taskRegistrar.addCronTask(
               cleaner::cleanup,
               properties.cleaner().cron()
         );
      };
   }
}
