package com.ecom.catalog.config;

import com.ecom.inbox.internal.aspect.InboxDeduplicationAspect;
import com.mongodb.client.MongoDatabase;
import com.ecom.infra.client.mongodb.MongoClients;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class CatalogConfiguration {

   @Bean
   Clock clock() {
      return Clock.systemUTC();
   }

   /**
    * The database of this service on the MongoDB client the service chose with {@code catalog.mongodb.client}.
    */
   @Bean
   MongoDatabase catalogDatabase(MongoClients clients,
                                 @Value("${catalog.mongodb.client}") String client,
                                 @Value("${catalog.mongodb.database:}") String database) {
      clients.requireDefined(client);
      String name = !database.isBlank() ? database.trim() : clients.database(client)
            .orElseThrow(() -> new IllegalStateException(
                  "catalog.mongodb.database is not set and ecom.client.mongodb." + client + ".database is not either"));
      return clients.client(client).getDatabase(name);
   }

   /**
    * The inbox module wants a transaction template even though catalog has no transactions (no SQL database); nothing
    * in catalog runs inside it.
    */
   @Bean
   TransactionTemplate inboxTransactionTemplate() {
      return new TransactionTemplate(new PlatformTransactionManager() {
         @Override
         public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
         }

         @Override
         public void commit(TransactionStatus status) {
         }

         @Override
         public void rollback(TransactionStatus status) {
         }
      });
   }

   /**
    * The inbox module records every message id in a SQL table to drop duplicates, and catalog has no SQL database.
    * Duplicates and reordered events are harmless here anyway, because an event that is not newer than the stored
    * version changes nothing, so the events go straight to the projection.
    */
   @Bean
   InboxDeduplicationAspect inboxDeduplicationAspect() {
      return new PassThroughInbox();
   }

   static class PassThroughInbox extends InboxDeduplicationAspect {

      PassThroughInbox() {
         super(null, null, null, null);
      }

      @Override
      public Object interceptUseCase(ProceedingJoinPoint joinPoint) throws Throwable {
         return joinPoint.proceed();
      }
   }
}
