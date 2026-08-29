package com.ecom.inbox.internal.config;

import com.ecom.inbox.InboxCleaner;
import com.ecom.inbox.InboxListener;
import com.ecom.inbox.InboxUseCase;
import com.ecom.inbox.internal.aspect.InboxDeduplicationAspect;
import com.ecom.inbox.internal.clean.DefaultInboxCleaner;
import com.ecom.inbox.internal.listener.DefaultInboxListener;
import com.ecom.inbox.internal.persistence.InboxRepository;
import com.ecom.shared.json.JsonMappers;
import com.ecom.shared.messaging.DomainEventRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

/**
 * Spring Boot auto-configuration for the Transactional Inbox module.
 */
@AutoConfiguration
@AutoConfigurationPackage(basePackages = "com.ecom.inbox")
@EnableConfigurationProperties(InboxProperties.class)
@EnableAspectJAutoProxy
@EnableScheduling
public class InboxAutoConfiguration {

   @Bean
   @ConditionalOnMissingBean
   public TransactionTemplate getInboxTransactionTemplate(
         ObjectProvider<PlatformTransactionManager> transactionManagerProvider
   ) {
      return Optional.ofNullable(transactionManagerProvider.getIfAvailable())
            .map(TransactionTemplate::new)
            .orElseGet(TransactionTemplate::new);
   }

   @Bean
   @ConditionalOnMissingBean
   public InboxDeduplicationAspect inboxDeduplicationAspect(
         ObjectProvider<InboxRepository> inboxRepositoryProvider,
         ObjectProvider<DomainEventRegistry> eventRegistryProvider,
         TransactionTemplate transactionTemplate,
         InboxProperties properties
   ) {
      DomainEventRegistry registry = eventRegistryProvider.getIfAvailable(DomainEventRegistry::new);
      return new InboxDeduplicationAspect(inboxRepositoryProvider.getObject(), registry, transactionTemplate, properties);
   }

   @Bean
   @ConditionalOnMissingBean
   public InboxListener inboxListener(
         ObjectProvider<DomainEventRegistry> eventRegistryProvider,
         ObjectProvider<InboxUseCase<?>> useCasesProvider
   ) {
      DomainEventRegistry registry = eventRegistryProvider.getIfAvailable(DomainEventRegistry::new);
      return new DefaultInboxListener(registry, JsonMappers.messaging(), useCasesProvider);
   }

   @Bean
   @ConditionalOnMissingBean
   @ConditionalOnProperty(prefix = "inbox.cleaner", name = "enabled", havingValue = "true", matchIfMissing = true)
   public InboxCleaner inboxCleaner(
         ObjectProvider<InboxRepository> inboxRepositoryProvider,
         TransactionTemplate transactionTemplate
   ) {
      return new DefaultInboxCleaner(inboxRepositoryProvider.getObject(), transactionTemplate);
   }

   @Configuration(proxyBeanMethods = false)
   @ConditionalOnProperty(prefix = "inbox.cleaner", name = "enabled", havingValue = "true", matchIfMissing = true)
   public static class InboxCleanerSchedulingConfiguration {

      @Bean
      @ConditionalOnMissingBean(name = "inboxSchedulingConfigurer")
      public SchedulingConfigurer inboxSchedulingConfigurer(
            InboxCleaner cleaner,
            InboxProperties properties
      ) {
         return taskRegistrar -> taskRegistrar.addCronTask(
               cleaner::cleanup,
               properties.cleaner().cron()
         );
      }
   }
}
