package com.ecom.infra.adapter.inbox;

import com.ecom.inbox.InboxListener;
import com.ecom.infra.client.kafka.KafkaClients;
import com.ecom.shared.json.JsonMappers;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Retry, backoff and dead-lettering belong to spring-kafka's listener container; these tests cover only what the
 * adapter itself decides: how a record becomes an inbox call, and when the consumer is created.
 */
class KafkaInboxConsumerTest {

   private static final String ENVELOPE =
       "{\"aggregate\":\"product\",\"action\":\"created\",\"data\":{\"productId\":\"p-1\"}}";

   private final List<String> received = new ArrayList<>();

   private final InboxListener listener = (messageId, signature, payload) ->
       received.add(messageId + "|" + signature + "|" + payload);

   private KafkaInboxConsumer consumer(InboxListener inboxListener) {
      return new KafkaInboxConsumer(inboxListener, () -> null, () -> null, JsonMappers.messaging(),
          "ecom.", "group", 3, 1);
   }

   private static ConsumerRecord<String, String> record(String value) {
      return new ConsumerRecord<>("ecom.product", 0, 7L, "p-1", value);
   }

   @Test
   void handsTheEnvelopeToTheListenerWithTheMessageIdFromTheHeader() {
      ConsumerRecord<String, String> record = record(ENVELOPE);
      record.headers().add("message-id", "m-1".getBytes(StandardCharsets.UTF_8));

      consumer(listener).onMessage(record);

      assertThat(received).containsExactly("m-1|product.created|{\"productId\":\"p-1\"}");
   }

   @Test
   void fallsBackToTopicPartitionOffsetAsTheMessageId() {
      consumer(listener).onMessage(record(ENVELOPE));

      assertThat(received).containsExactly("ecom.product-0-7|product.created|{\"productId\":\"p-1\"}");
   }

   @Test
   void aMalformedEnvelopeIsRejectedAsNotRetryableWithoutCallingTheListener() {
      assertThatThrownBy(() -> consumer(listener).onMessage(record("{\"aggregate\":\"product\"}")))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Malformed envelope");
      assertThat(received).isEmpty();
   }

   @Test
   void unreadableJsonIsRejectedAsNotRetryable() {
      assertThatThrownBy(() -> consumer(listener).onMessage(record("not json")))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Unreadable message");
      assertThat(received).isEmpty();
   }

   @Test
   void aListenerFailureIsLeftToTheContainerToRetry() {
      InboxListener failing = (id, signature, payload) -> {
         throw new IllegalStateException("handler failed");
      };

      assertThatThrownBy(() -> consumer(failing).onMessage(record(ENVELOPE)))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("handler failed");
   }

   @Test
   void configurationNeedsTheChoiceAndAListener() {
      InboxListener noSubscriptions = new InboxListener() {
         public void listen(String id, String signature, String payload) { }
         public Set<String> subscribedAggregates() { return Set.of(); }
      };
      ApplicationContextRunner runner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(KafkaInboxAdapterConfiguration.class))
          .withBean(InboxListener.class, () -> noSubscriptions)
          .withBean(KafkaClients.class, () -> Mockito.mock(KafkaClients.class));

      runner.run(context -> assertThat(context).doesNotHaveBean(KafkaInboxConsumer.class));
      runner.withPropertyValues("ecom.inbox.client=sqs.orders")
          .run(context -> assertThat(context).doesNotHaveBean(KafkaInboxConsumer.class));
      runner.withPropertyValues("ecom.inbox.client=kafka.core").run(context -> {
         assertThat(context).hasSingleBean(KafkaInboxConsumer.class);
         // no inbox use case subscribes to anything, so no container is started
         assertThat(context.getBean(KafkaInboxConsumer.class).isRunning()).isFalse();
      });

      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(KafkaInboxAdapterConfiguration.class))
          .withBean(KafkaClients.class, () -> Mockito.mock(KafkaClients.class))
          .withPropertyValues("ecom.inbox.client=kafka.core")
          .run(context -> assertThat(context).doesNotHaveBean(KafkaInboxConsumer.class));
   }
}
