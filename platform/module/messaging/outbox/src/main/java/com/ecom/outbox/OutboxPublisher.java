package com.ecom.outbox;

/**
 * Extension SPI implemented by the host application (or default transport starter)
 * to publish outbox messages to external message brokers (e.g. Kafka, RabbitMQ).
 */
@FunctionalInterface
public interface OutboxPublisher {

   /**
     * Publishes a message to the target message broker.
     *
     * @param message the immutable outbox message to publish
     * @throws Exception if message dispatch fails, triggering retry backoff
     */
   void publish(OutboxMessage message) throws Exception;
}
