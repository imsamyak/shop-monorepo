package com.ecom.infra.adapter.inbox;

import com.ecom.inbox.InboxListener;
import com.ecom.infra.adapter.ClientReference;
import com.ecom.infra.client.rabbit.RabbitClients;
import com.ecom.shared.json.JsonMappers;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Feeds the inbox from the RabbitMQ client the service named with {@code ecom.inbox.client: rabbit.<name>}. Exchanges
 * follow the outbox adapter's layout ({@code <prefix><aggregate>}). A consumer bean defined by the service replaces
 * this one.
 */
@AutoConfiguration(afterName = {
    "com.ecom.inbox.internal.config.InboxAutoConfiguration",
    "com.ecom.infra.client.rabbit.RabbitClientConfiguration"})
@ConditionalOnClass({InboxListener.class, SimpleMessageListenerContainer.class})
@ConditionalOnExpression("'${ecom.inbox.client:}'.toLowerCase().startsWith('rabbit.')")
public class RabbitInboxAdapterConfiguration {

   @Bean
   @ConditionalOnBean(InboxListener.class)
   @ConditionalOnMissingBean
   RabbitInboxConsumer rabbitInboxConsumer(
       InboxListener listener,
       RabbitClients clients,
       Environment environment,
       @Value("${ecom.adapter.rabbit.exchange-prefix:ecom.}") String exchangePrefix,
       @Value("${ecom.adapter.rabbit.consumer.group:${spring.application.name:ecom-inbox}}") String group,
       @Value("${ecom.adapter.rabbit.consumer.max-attempts:8}") int maxAttempts,
       @Value("${ecom.adapter.rabbit.consumer.backoff-ms:200}") long backoffMs,
       @Value("${ecom.adapter.rabbit.consumer.prefetch:10}") int prefetch) {
      String client = ClientReference.of(environment, "inbox").name();
      clients.requireDefined(client);
      return new RabbitInboxConsumer(listener, () -> clients.connectionFactory(client), () -> clients.admin(client),
          () -> clients.template(client), JsonMappers.messaging(), exchangePrefix, group, maxAttempts, backoffMs,
          prefetch);
   }
}
