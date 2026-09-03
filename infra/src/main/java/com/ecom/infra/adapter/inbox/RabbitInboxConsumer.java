package com.ecom.infra.adapter.inbox;

import com.ecom.inbox.InboxListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.SmartLifecycle;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Plugs a RabbitMQ listener container into the inbox. For every aggregate this service has an inbox use case for it
 * declares the aggregate's exchange, a durable queue {@code <group>.<aggregate>} bound to it, and a dead-letter queue
 * {@code <group>.<aggregate>.dlq}, then hands each message to the {@link InboxListener}.
 * <p>
 * The queue is this consumer's position: the broker keeps what it has not yet acknowledged, so several replicas that
 * share a group share the queue and split its messages, while a different group gets its own copy of every message.
 * A message is acknowledged after it was handled, so delivery is at-least-once and the inbox makes processing
 * idempotent. A failing message is retried here with exponential backoff, which keeps the order of the queue, and is
 * then moved to the dead-letter queue; a message that cannot be parsed goes there at once.
 */
public class RabbitInboxConsumer implements SmartLifecycle {

   static final String HEADER_MESSAGE_ID = "message-id";
   static final String HEADER_ERROR = "x-error";

   private static final Logger log = LoggerFactory.getLogger(RabbitInboxConsumer.class);

   private final InboxListener listener;
   private final Supplier<ConnectionFactory> connectionFactory;
   private final Supplier<RabbitAdmin> admin;
   private final Supplier<RabbitTemplate> template;
   private final ObjectMapper objectMapper;
   private final String exchangePrefix;
   private final String group;
   private final int maxAttempts;
   private final long backoffMs;
   private final int prefetch;

   private SimpleMessageListenerContainer container;

   RabbitInboxConsumer(InboxListener listener,
                       Supplier<ConnectionFactory> connectionFactory,
                       Supplier<RabbitAdmin> admin,
                       Supplier<RabbitTemplate> template,
                       ObjectMapper objectMapper,
                       String exchangePrefix,
                       String group,
                       int maxAttempts,
                       long backoffMs,
                       int prefetch) {
      this.listener = listener;
      this.connectionFactory = connectionFactory;
      this.admin = admin;
      this.template = template;
      this.objectMapper = objectMapper;
      this.exchangePrefix = exchangePrefix;
      this.group = group;
      this.maxAttempts = maxAttempts;
      this.backoffMs = backoffMs;
      this.prefetch = prefetch;
   }

   @Override
   public synchronized void start() {
      if (container != null) {
         return;
      }
      Set<String> aggregates = listener.subscribedAggregates();
      if (aggregates.isEmpty()) {
         log.info("No inbox use cases registered; RabbitMQ inbox consumer not started");
         return;
      }
      RabbitAdmin rabbitAdmin = admin.get();
      String[] queues = aggregates.stream().map(aggregate -> declare(rabbitAdmin, aggregate)).toArray(String[]::new);

      container = new SimpleMessageListenerContainer(connectionFactory.get());
      container.setQueueNames(queues);
      container.setAcknowledgeMode(AcknowledgeMode.AUTO);
      container.setConcurrentConsumers(1);
      container.setPrefetchCount(prefetch);
      container.setMissingQueuesFatal(false);
      container.setMessageListener((MessageListener) this::onMessage);
      container.start();
      log.info("RabbitMQ inbox consumer started for {}", String.join(",", queues));
   }

   private String declare(RabbitAdmin rabbitAdmin, String aggregate) {
      String queueName = group + "." + aggregate;
      TopicExchange exchange = new TopicExchange(exchangePrefix + aggregate, true, false);
      Queue queue = new Queue(queueName, true);
      Binding binding = BindingBuilder.bind(queue).to(exchange).with("#");
      rabbitAdmin.declareExchange(exchange);
      rabbitAdmin.declareQueue(queue);
      rabbitAdmin.declareQueue(new Queue(deadLetterQueue(queueName), true));
      rabbitAdmin.declareBinding(binding);
      return queueName;
   }

   void onMessage(Message message) {
      String body = new String(message.getBody(), StandardCharsets.UTF_8);
      String messageId = messageId(message);
      String location = location(message);
      long delay = backoffMs;
      for (int attempt = 1; ; attempt++) {
         try {
            InboxEnvelopes.dispatch(listener, objectMapper, messageId, body, location);
            return;
         } catch (IllegalArgumentException e) {
            deadLetter(message, e);
            return;
         } catch (Exception e) {
            if (attempt >= maxAttempts) {
               deadLetter(message, e);
               return;
            }
            log.warn("Handling {} failed (attempt {}/{}): {}", location, attempt, maxAttempts, e.getMessage());
            pause(delay);
            delay = Math.min(delay * 2, 10_000L);
         }
      }
   }

   private void deadLetter(Message message, Exception cause) {
      String queue = message.getMessageProperties().getConsumerQueue();
      String error = cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
      log.error("Moving {} to the dead-letter queue: {}", location(message), error);
      Message dead = MessageBuilder.fromMessage(message)
          .setHeader(HEADER_ERROR, error.length() > 1000 ? error.substring(0, 1000) : error)
          .build();
      template.get().send("", deadLetterQueue(queue), dead);
   }

   private static void pause(long millis) {
      try {
         Thread.sleep(millis);
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         throw new IllegalStateException("Interrupted while waiting to retry", e);
      }
   }

   static String deadLetterQueue(String queue) {
      return queue + ".dlq";
   }

   private static String messageId(Message message) {
      Object header = message.getMessageProperties().getHeader(HEADER_MESSAGE_ID);
      if (header != null) {
         return header.toString();
      }
      String id = message.getMessageProperties().getMessageId();
      return id != null ? id : location(message);
   }

   private static String location(Message message) {
      return message.getMessageProperties().getConsumerQueue() + "-" + message.getMessageProperties().getDeliveryTag();
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
