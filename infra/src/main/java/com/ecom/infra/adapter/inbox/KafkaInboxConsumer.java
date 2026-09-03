package com.ecom.infra.adapter.inbox;

import com.ecom.inbox.InboxListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.util.backoff.ExponentialBackOff;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Plugs spring-kafka's listener container into the inbox: consumes the topics of every aggregate this service has an
 * inbox use case for and hands each message to the {@link InboxListener}. spring-kafka retries a failing message with
 * exponential backoff and then publishes it to {@code <topic>.DLT}; a message that cannot be parsed goes straight
 * there. Offsets are committed per record after it was handled, so delivery is at-least-once and the inbox makes
 * processing idempotent.
 */
public class KafkaInboxConsumer implements SmartLifecycle {

   static final String HEADER_MESSAGE_ID = "message-id";

   private static final Logger log = LoggerFactory.getLogger(KafkaInboxConsumer.class);

   private final InboxListener listener;
   private final Supplier<ConsumerFactory<String, String>> consumerFactory;
   private final Supplier<KafkaTemplate<String, String>> deadLetterTemplate;
   private final ObjectMapper objectMapper;
   private final String topicPrefix;
   private final String groupId;
   private final int maxAttempts;
   private final long backoffMs;

   private ConcurrentMessageListenerContainer<String, String> container;

   KafkaInboxConsumer(InboxListener listener,
                      Supplier<ConsumerFactory<String, String>> consumerFactory,
                      Supplier<KafkaTemplate<String, String>> deadLetterTemplate,
                      ObjectMapper objectMapper,
                      String topicPrefix,
                      String groupId,
                      int maxAttempts,
                      long backoffMs) {
      this.listener = listener;
      this.consumerFactory = consumerFactory;
      this.deadLetterTemplate = deadLetterTemplate;
      this.objectMapper = objectMapper;
      this.topicPrefix = topicPrefix;
      this.groupId = groupId;
      this.maxAttempts = maxAttempts;
      this.backoffMs = backoffMs;
   }

   @Override
   public synchronized void start() {
      if (container != null) {
         return;
      }
      Set<String> aggregates = listener.subscribedAggregates();
      if (aggregates.isEmpty()) {
         log.info("No inbox use cases registered; Kafka inbox consumer not started");
         return;
      }
      String[] topics = aggregates.stream().map(aggregate -> topicPrefix + aggregate).toArray(String[]::new);

      ContainerProperties properties = new ContainerProperties(topics);
      properties.setGroupId(groupId);
      properties.setAckMode(ContainerProperties.AckMode.RECORD);
      properties.setMissingTopicsFatal(false);
      properties.setMessageListener((MessageListener<String, String>) this::onMessage);

      ExponentialBackOff backOff = new ExponentialBackOff(backoffMs, 2.0);
      backOff.setMaxInterval(10_000L);
      backOff.setMaxAttempts(maxAttempts);
      DefaultErrorHandler errorHandler = new DefaultErrorHandler(
          new DeadLetterPublishingRecoverer(deadLetterTemplate.get(),
              (record, ex) -> new TopicPartition(record.topic() + ".DLT", -1)),
          backOff);
      errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);

      container = new ConcurrentMessageListenerContainer<>(consumerFactory.get(), properties);
      container.setCommonErrorHandler(errorHandler);
      container.start();
      log.info("Kafka inbox consumer started for {} (group {})", String.join(",", topics), groupId);
   }

   void onMessage(ConsumerRecord<String, String> record) {
      InboxEnvelopes.dispatch(listener, objectMapper, messageId(record), record.value(), location(record));
   }

   private static String messageId(ConsumerRecord<String, String> record) {
      Header header = record.headers().lastHeader(HEADER_MESSAGE_ID);
      if (header != null && header.value() != null) {
         return new String(header.value(), StandardCharsets.UTF_8);
      }
      return location(record);
   }

   private static String location(ConsumerRecord<String, String> record) {
      return record.topic() + "-" + record.partition() + "-" + record.offset();
   }

   @Override
   public synchronized void stop() {
      if (container != null) {
         container.stop();
         container = null;
      }
   }

   @Override
   public synchronized boolean isRunning() {
      return container != null && container.isRunning();
   }
}
