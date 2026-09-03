package com.ecom.infra.adapter.outbox;

import com.ecom.infra.client.rabbit.RabbitClientConfiguration;
import com.ecom.infra.client.rabbit.RabbitClients;
import com.ecom.outbox.OutboxMessage;
import com.ecom.outbox.OutboxPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RabbitOutboxAdapterConfigurationTest {

   private final ApplicationContextRunner runner = new ApplicationContextRunner()
       .withConfiguration(AutoConfigurations.of(RabbitOutboxAdapterConfiguration.class));

   private static OutboxMessage message(UUID id) {
      return new OutboxMessage() {
         public UUID id() { return id; }
         public String aggregateType() { return "product"; }
         public String aggregateId() { return "p-1"; }
         public String payload() { return "{\"aggregate\":\"product\"}"; }
         public Instant createdAt() { return Instant.now(); }
      };
   }

   /** A template whose {@code invoke} runs the callback against a mock that confirms every publish. */
   private static RabbitTemplate templateThatConfirms(RabbitOperations operations) {
      RabbitTemplate template = Mockito.mock(RabbitTemplate.class);
      Mockito.when(template.invoke(Mockito.any())).thenAnswer(call ->
          ((RabbitOperations.OperationsCallback<?>) call.getArgument(0)).doInRabbit(operations));
      return template;
   }

   private static RabbitClients clientsWith(String name, RabbitTemplate template, RabbitAdmin admin) {
      RabbitClients clients = Mockito.mock(RabbitClients.class);
      Mockito.when(clients.template(name)).thenReturn(template);
      Mockito.when(clients.admin(name)).thenReturn(admin);
      return clients;
   }

   @Test
   void createsNoPublisherUnlessTheServiceChoosesRabbit() {
      runner.run(context -> assertThat(context).doesNotHaveBean(OutboxPublisher.class));
      runner.withPropertyValues("ecom.outbox.client=kafka.core")
          .run(context -> assertThat(context).doesNotHaveBean(OutboxPublisher.class));
   }

   @Test
   void publishesToTheAggregateExchangeWithTheAggregateIdAsRoutingKeyAndTheMessageId() throws Exception {
      RabbitOperations operations = Mockito.mock(RabbitOperations.class);
      RabbitAdmin admin = Mockito.mock(RabbitAdmin.class);
      UUID id = UUID.randomUUID();

      runner.withBean(RabbitClients.class, () -> clientsWith("core", templateThatConfirms(operations), admin))
          .withPropertyValues("ecom.outbox.client=rabbit.core")
          .run(context -> context.getBean(OutboxPublisher.class).publish(message(id)));

      ArgumentCaptor<TopicExchange> exchange = ArgumentCaptor.forClass(TopicExchange.class);
      Mockito.verify(admin).declareExchange(exchange.capture());
      assertThat(exchange.getValue().getName()).isEqualTo("ecom.product");
      assertThat(exchange.getValue().isDurable()).isTrue();

      ArgumentCaptor<Message> sent = ArgumentCaptor.forClass(Message.class);
      Mockito.verify(operations).send(Mockito.eq("ecom.product"), Mockito.eq("p-1"), sent.capture());
      Mockito.verify(operations).waitForConfirmsOrDie(10000L);
      assertThat(new String(sent.getValue().getBody(), StandardCharsets.UTF_8)).isEqualTo("{\"aggregate\":\"product\"}");
      assertThat(sent.getValue().getMessageProperties().getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
      assertThat(sent.getValue().getMessageProperties().<String>getHeader("message-id")).isEqualTo(id.toString());
      assertThat(sent.getValue().getMessageProperties().getMessageId()).isEqualTo(id.toString());
   }

   @Test
   void declaresAnExchangeOnlyOnce() throws Exception {
      RabbitOperations operations = Mockito.mock(RabbitOperations.class);
      RabbitAdmin admin = Mockito.mock(RabbitAdmin.class);

      runner.withBean(RabbitClients.class, () -> clientsWith("core", templateThatConfirms(operations), admin))
          .withPropertyValues("ecom.outbox.client=rabbit.core")
          .run(context -> {
             OutboxPublisher publisher = context.getBean(OutboxPublisher.class);
             publisher.publish(message(UUID.randomUUID()));
             publisher.publish(message(UUID.randomUUID()));
          });

      Mockito.verify(admin, Mockito.times(1)).declareExchange(Mockito.any(TopicExchange.class));
   }

   @Test
   void exchangePrefixIsConfigurable() throws Exception {
      RabbitOperations operations = Mockito.mock(RabbitOperations.class);

      runner.withBean(RabbitClients.class,
              () -> clientsWith("core", templateThatConfirms(operations), Mockito.mock(RabbitAdmin.class)))
          .withPropertyValues("ecom.outbox.client=rabbit.core", "ecom.adapter.rabbit.exchange-prefix=shop.")
          .run(context -> context.getBean(OutboxPublisher.class).publish(message(UUID.randomUUID())));

      Mockito.verify(operations).send(Mockito.eq("shop.product"), Mockito.eq("p-1"), Mockito.any(Message.class));
   }

   @Test
   void aBrokerThatDoesNotConfirmSurfacesSoTheOutboxRetries() throws Exception {
      RabbitOperations operations = Mockito.mock(RabbitOperations.class);
      Mockito.doThrow(new IllegalStateException("nack")).when(operations).waitForConfirmsOrDie(Mockito.anyLong());

      runner.withBean(RabbitClients.class,
              () -> clientsWith("core", templateThatConfirms(operations), Mockito.mock(RabbitAdmin.class)))
          .withPropertyValues("ecom.outbox.client=rabbit.core")
          .run(context -> assertThatThrownBy(() -> context.getBean(OutboxPublisher.class).publish(message(UUID.randomUUID())))
              .hasStackTraceContaining("nack"));
   }

   @Test
   void aChoiceWithoutANameFailsStartup() {
      runner.withBean(RabbitClients.class, () -> Mockito.mock(RabbitClients.class))
          .withPropertyValues("ecom.outbox.client=rabbit.")
          .run(context -> assertThat(context).hasFailed());
   }

   @Test
   void publisherDefinedByTheServiceWins() {
      OutboxPublisher own = m -> { };
      runner.withBean(OutboxPublisher.class, () -> own)
          .withBean(RabbitClients.class, () -> Mockito.mock(RabbitClients.class))
          .withPropertyValues("ecom.outbox.client=rabbit.core")
          .run(context -> assertThat(context.getBean(OutboxPublisher.class)).isSameAs(own));
   }

   @Configuration(proxyBeanMethods = false)
   @Import({RabbitClientConfiguration.class, RabbitOutboxAdapterConfiguration.class})
   static class ServiceUsingTheOutbox {
   }

   @Test
   void theServiceNamesTheClientAndTheProfileSaysWhereItIs() {
      try (var context = new SpringApplicationBuilder(ServiceUsingTheOutbox.class)
          .web(WebApplicationType.NONE)
          .profiles("local")
          .properties("ecom.outbox.client=rabbit.core")
          .run()) {
         assertThat(context.getBean(OutboxPublisher.class)).isNotNull();
      }
   }

   @Test
   void aNameTheProfileDoesNotDefineFailsStartupAndListsTheOnesItDoes() {
      assertThatThrownBy(() -> new SpringApplicationBuilder(ServiceUsingTheOutbox.class)
          .web(WebApplicationType.NONE)
          .profiles("local")
          .properties("ecom.outbox.client=rabbit.nope")
          .run())
          .hasStackTraceContaining("No RabbitMQ client named 'nope'")
          .hasStackTraceContaining("Available: [core]");
   }
}
