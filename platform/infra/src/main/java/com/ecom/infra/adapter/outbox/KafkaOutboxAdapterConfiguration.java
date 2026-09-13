package com.ecom.infra.adapter.outbox;

import com.ecom.infra.adapter.ClientReference;
import com.ecom.infra.client.kafka.KafkaClients;
import com.ecom.outbox.OutboxPublisher;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Publishes outbox messages to the Kafka client the service named with {@code ecom.outbox.client: kafka.<name>}.
 * One topic per aggregate ({@code <prefix><aggregate>}) and the aggregate id as the record key, so every event of
 * one aggregate lands on the same partition and is consumed in order. The send is awaited, so the outbox row is only
 * marked processed once the broker acknowledged it. A publisher bean defined by the service replaces this one.
 */
@AutoConfiguration(afterName = "com.ecom.infra.client.kafka.KafkaClientConfiguration")
@ConditionalOnClass({OutboxPublisher.class, KafkaTemplate.class})
@ConditionalOnExpression("'${ecom.outbox.client:}'.toLowerCase().startsWith('kafka.')")
public class KafkaOutboxAdapterConfiguration {

   static final String HEADER_MESSAGE_ID = "message-id";

   @Bean
   @ConditionalOnMissingBean(OutboxPublisher.class)
   OutboxPublisher kafkaOutboxPublisher(KafkaClients clients,
                                        Environment environment,
                                        @Value("${ecom.adapter.kafka.topic-prefix:ecom.}") String topicPrefix,
                                        @Value("${ecom.adapter.kafka.send-timeout-ms:10000}") long sendTimeoutMs) {
      String client = ClientReference.of(environment, "outbox").name();
      clients.requireDefined(client);
      return message -> {
         ProducerRecord<String, String> record =
             new ProducerRecord<>(topicPrefix + message.aggregateType(), message.aggregateId(), message.payload());
         record.headers().add(HEADER_MESSAGE_ID, message.id().toString().getBytes(StandardCharsets.UTF_8));
         clients.template(client).send(record).get(sendTimeoutMs, TimeUnit.MILLISECONDS);
      };
   }
}
