package com.ecom.infra.adapter.inbox;

import com.ecom.inbox.InboxListener;
import com.ecom.infra.adapter.ClientReference;
import com.ecom.infra.client.kafka.KafkaClients;
import com.ecom.shared.json.JsonMappers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

/**
 * Feeds the inbox from the Kafka client the service named with {@code ecom.inbox.client: kafka.<name>}. Topics follow
 * the outbox adapter's layout ({@code <prefix><aggregate>}). A consumer bean defined by the service replaces this one.
 */
@AutoConfiguration(afterName = {
    "com.ecom.inbox.internal.config.InboxAutoConfiguration",
    "com.ecom.infra.client.kafka.KafkaClientConfiguration"})
@ConditionalOnClass({InboxListener.class, ConcurrentMessageListenerContainer.class})
@ConditionalOnExpression("'${ecom.inbox.client:}'.toLowerCase().startsWith('kafka.')")
public class KafkaInboxAdapterConfiguration {

   @Bean
   @ConditionalOnBean(InboxListener.class)
   @ConditionalOnMissingBean
   KafkaInboxConsumer kafkaInboxConsumer(
       InboxListener listener,
       KafkaClients clients,
       Environment environment,
       @Value("${ecom.adapter.kafka.topic-prefix:ecom.}") String topicPrefix,
       @Value("${ecom.adapter.kafka.consumer.group-id:${spring.application.name:ecom-inbox}}") String groupId,
       @Value("${ecom.adapter.kafka.consumer.max-attempts:8}") int maxAttempts,
       @Value("${ecom.adapter.kafka.consumer.backoff-ms:200}") long backoffMs) {
      String client = ClientReference.of(environment, "inbox").name();
      clients.requireDefined(client);
      return new KafkaInboxConsumer(listener, () -> clients.consumerFactory(client), () -> clients.template(client),
          JsonMappers.messaging(), topicPrefix, groupId, maxAttempts, backoffMs);
   }
}
