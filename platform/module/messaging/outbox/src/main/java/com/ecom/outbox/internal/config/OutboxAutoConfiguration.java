package com.ecom.outbox.internal.config;

import com.ecom.outbox.OutboxEventFactory;
import com.ecom.outbox.internal.aspect.DefaultOutboxEventFactory;
import com.ecom.outbox.internal.aspect.OutboxAspect;
import com.ecom.outbox.internal.persistence.OutboxRepository;
import com.ecom.shared.json.JsonMappers;
import com.ecom.shared.messaging.DomainEventRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

/**
 * Spring Boot auto-configuration for the Outbox module.
 */
@AutoConfiguration
@AutoConfigurationPackage(basePackages = "com.ecom.outbox")
@EnableConfigurationProperties(OutboxProperties.class)
@EnableAspectJAutoProxy
@EnableScheduling
@Import(OutboxRelayConfiguration.class)
public class OutboxAutoConfiguration {

   @Bean
   @ConditionalOnMissingBean
   public OutboxEventFactory getOutboxEventFactory(
         ObjectProvider<DomainEventRegistry> domainEventRegistryProvider,
         OutboxProperties properties
   ) {
      DomainEventRegistry registry = domainEventRegistryProvider.getIfAvailable(DomainEventRegistry::new);
      return new DefaultOutboxEventFactory(JsonMappers.messaging(), registry, properties.maxPayloadLength());
   }

   @Bean
   @ConditionalOnMissingBean
   public OutboxAspect getOutboxAspect(
         ObjectProvider<OutboxRepository> outboxRepositoryProvider,
         OutboxEventFactory outboxEventFactory,
         TransactionTemplate transactionTemplate,
         ApplicationEventPublisher eventPublisher
   ) {
      return new OutboxAspect(outboxRepositoryProvider.getObject(), outboxEventFactory, transactionTemplate, eventPublisher);
   }

   @Bean
   @ConditionalOnMissingBean
   public TransactionTemplate getOutboxTransactionTemplate(
         ObjectProvider<PlatformTransactionManager> transactionManagerProvider
   ) {
      return Optional.ofNullable(transactionManagerProvider.getIfAvailable())
            .map(TransactionTemplate::new)
            .orElseGet(TransactionTemplate::new);
   }
}
