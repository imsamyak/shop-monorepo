package com.ecom.infra.adapter.inbox;

import com.ecom.inbox.InboxListener;
import com.ecom.infra.client.rabbit.RabbitClients;
import com.ecom.shared.json.JsonMappers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Declaring queues and running the listener container is spring-amqp's job; these tests cover what the adapter itself
 * decides: how a message becomes an inbox call, when it is retried, and when it is moved to the dead-letter queue.
 */
class RabbitInboxConsumerTest {

   private static final String ENVELOPE =
       "{\"aggregate\":\"product\",\"action\":\"created\",\"data\":{\"productId\":\"p-1\"}}";

   private final List<String> received = new ArrayList<>();
   private final RabbitTemplate template = Mockito.mock(RabbitTemplate.class);

   private final InboxListener listener = (messageId, signature, payload) ->
       received.add(messageId + "|" + signature + "|" + payload);

   private RabbitInboxConsumer consumer(InboxListener inboxListener, int maxAttempts) {
      return new RabbitInboxConsumer(inboxListener, () -> null, () -> null, () -> template, JsonMappers.messaging(),
          "ecom.", "inventory", maxAttempts, 1, 10);
   }

   private static Message message(String body, String messageIdHeader) {
      MessageBuilder builder = MessageBuilder.withBody(body.getBytes(StandardCharsets.UTF_8));
      if (messageIdHeader != null) {
         builder.setHeader("message-id", messageIdHeader);
      }
      Message message = builder.build();
      message.getMessageProperties().setConsumerQueue("inventory.product");
      message.getMessageProperties().setDeliveryTag(7L);
      return message;
   }

   private Message deadLettered() {
      ArgumentCaptor<Message> dead = ArgumentCaptor.forClass(Message.class);
      Mockito.verify(template).send(Mockito.eq(""), Mockito.eq("inventory.product.dlq"), dead.capture());
      return dead.getValue();
   }

   @Test
   void handsTheEnvelopeToTheListenerWithTheMessageIdFromTheHeader() {
      consumer(listener, 3).onMessage(message(ENVELOPE, "m-1"));

      assertThat(received).containsExactly("m-1|product.created|{\"productId\":\"p-1\"}");
      Mockito.verifyNoInteractions(template);
   }

   @Test
   void fallsBackToQueueAndDeliveryTagAsTheMessageId() {
      consumer(listener, 3).onMessage(message(ENVELOPE, null));

      assertThat(received).containsExactly("inventory.product-7|product.created|{\"productId\":\"p-1\"}");
   }

   @Test
   void aMalformedEnvelopeGoesStraightToTheDeadLetterQueueWithoutCallingTheListener() {
      consumer(listener, 3).onMessage(message("{\"aggregate\":\"product\"}", "m-1"));

      assertThat(received).isEmpty();
      Message dead = deadLettered();
      assertThat(dead.getMessageProperties().<String>getHeader("x-error")).contains("Malformed envelope");
      assertThat(new String(dead.getBody(), StandardCharsets.UTF_8)).isEqualTo("{\"aggregate\":\"product\"}");
   }

   @Test
   void unreadableJsonGoesStraightToTheDeadLetterQueue() {
      consumer(listener, 3).onMessage(message("not json", "m-1"));

      assertThat(deadLettered().getMessageProperties().<String>getHeader("x-error")).contains("Unreadable message");
   }

   @Test
   void aFailingHandlerIsRetriedAndASuccessLeavesNothingInTheDeadLetterQueue() {
      int[] calls = {0};
      InboxListener flaky = (id, signature, payload) -> {
         if (++calls[0] < 3) {
            throw new IllegalStateException("temporary");
         }
         received.add(id);
      };

      consumer(flaky, 5).onMessage(message(ENVELOPE, "m-1"));

      assertThat(calls[0]).isEqualTo(3);
      assertThat(received).containsExactly("m-1");
      Mockito.verifyNoInteractions(template);
   }

   @Test
   void aHandlerThatKeepsFailingIsMovedToTheDeadLetterQueueAfterTheAttempts() {
      int[] calls = {0};
      InboxListener failing = (id, signature, payload) -> {
         calls[0]++;
         throw new IllegalStateException("handler failed");
      };

      consumer(failing, 3).onMessage(message(ENVELOPE, "m-1"));

      assertThat(calls[0]).isEqualTo(3);
      Message dead = deadLettered();
      assertThat(dead.getMessageProperties().<String>getHeader("x-error")).isEqualTo("handler failed");
      assertThat(dead.getMessageProperties().<String>getHeader("message-id")).isEqualTo("m-1");
   }

   @Test
   void configurationNeedsTheChoiceAndAListener() {
      InboxListener noSubscriptions = new InboxListener() {
         public void listen(String id, String signature, String payload) { }
         public Set<String> subscribedAggregates() { return Set.of(); }
      };
      ApplicationContextRunner runner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(RabbitInboxAdapterConfiguration.class))
          .withBean(InboxListener.class, () -> noSubscriptions)
          .withBean(RabbitClients.class, () -> Mockito.mock(RabbitClients.class));

      runner.run(context -> assertThat(context).doesNotHaveBean(RabbitInboxConsumer.class));
      runner.withPropertyValues("ecom.inbox.client=kafka.core")
          .run(context -> assertThat(context).doesNotHaveBean(RabbitInboxConsumer.class));
      runner.withPropertyValues("ecom.inbox.client=rabbit.core").run(context -> {
         assertThat(context).hasSingleBean(RabbitInboxConsumer.class);
         // no inbox use case subscribes to anything, so no container is started
         assertThat(context.getBean(RabbitInboxConsumer.class).isRunning()).isFalse();
      });

      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(RabbitInboxAdapterConfiguration.class))
          .withBean(RabbitClients.class, () -> Mockito.mock(RabbitClients.class))
          .withPropertyValues("ecom.inbox.client=rabbit.core")
          .run(context -> assertThat(context).doesNotHaveBean(RabbitInboxConsumer.class));
   }
}
