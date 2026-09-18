package com.ecom.infra.adapter.outbox;

import com.ecom.infra.adapter.ClientReference;
import com.ecom.infra.client.rabbit.RabbitClients;
import com.ecom.outbox.OutboxPublisher;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Publishes outbox messages to the RabbitMQ client the service named with {@code ecom.outbox.client: rabbit.<name>}.
 * One durable topic exchange per aggregate ({@code <prefix><aggregate>}), the aggregate id as the routing key and a
 * persistent message. The publish waits for the broker's confirmation, so the outbox row is only marked processed once
 * the broker accepted it. A publisher bean defined by the service replaces this one.
 * <p>
 * An exchange only forwards to queues that are bound to it: a message published before any consumer has declared its
 * queue is accepted by the broker and then dropped, unlike a Kafka topic that keeps it.
 */
@AutoConfiguration(afterName = "com.ecom.infra.client.rabbit.RabbitClientConfiguration")
@ConditionalOnClass({OutboxPublisher.class, RabbitTemplate.class})
@ConditionalOnExpression("'${ecom.outbox.client:}'.toLowerCase().startsWith('rabbit.')")
public class RabbitOutboxAdapterConfiguration {

   static final String HEADER_MESSAGE_ID = "message-id";

   @Bean
   @ConditionalOnMissingBean(OutboxPublisher.class)
   OutboxPublisher rabbitOutboxPublisher(RabbitClients clients,
                                         Environment environment,
                                         @Value("${ecom.adapter.rabbit.exchange-prefix:ecom.}") String exchangePrefix,
                                         @Value("${ecom.adapter.rabbit.confirm-timeout-ms:10000}") long confirmTimeoutMs) {
      String client = ClientReference.of(environment, "outbox").name();
      clients.requireDefined(client);
      Set<String> declared = ConcurrentHashMap.newKeySet();
      return message -> {
         String exchange = exchangePrefix + message.aggregateType();
         if (!declared.contains(exchange)) {
            clients.admin(client).declareExchange(new TopicExchange(exchange, true, false));
            declared.add(exchange);
         }
         Message amqp = MessageBuilder.withBody(message.payload().getBytes(StandardCharsets.UTF_8))
             .setContentType("application/json")
             .setContentEncoding("UTF-8")
             .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
             .setMessageId(message.id().toString())
             .setHeader(HEADER_MESSAGE_ID, message.id().toString())
             .build();
         clients.template(client).invoke(operations -> {
            operations.send(exchange, message.aggregateId(), amqp);
            operations.waitForConfirmsOrDie(confirmTimeoutMs);
            return null;
         });
      };
   }
}
