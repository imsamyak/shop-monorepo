package com.ecom.infra.adapter.outbox;

import com.ecom.infra.client.kafka.KafkaClientConfiguration;
import com.ecom.infra.client.kafka.KafkaClients;
import com.ecom.outbox.OutboxMessage;
import com.ecom.outbox.OutboxPublisher;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KafkaOutboxAdapterConfigurationTest {

   private final ApplicationContextRunner runner = new ApplicationContextRunner()
       .withConfiguration(AutoConfigurations.of(KafkaOutboxAdapterConfiguration.class));

   private static OutboxMessage message(UUID id) {
      return new OutboxMessage() {
         public UUID id() { return id; }
         public String aggregateType() { return "product"; }
         public String aggregateId() { return "p-1"; }
         public String payload() { return "{\"aggregate\":\"product\"}"; }
         public Instant createdAt() { return Instant.now(); }
      };
   }

   @SuppressWarnings("unchecked")
   private static KafkaTemplate<String, String> templateThatAcknowledges() {
      KafkaTemplate<String, String> template = Mockito.mock(KafkaTemplate.class);
      Mockito.when(template.send(Mockito.any(ProducerRecord.class)))
          .thenReturn(CompletableFuture.completedFuture(Mockito.mock(SendResult.class)));
      return template;
   }

   private static KafkaClients clientsWith(String name, KafkaTemplate<String, String> template) {
      KafkaClients clients = Mockito.mock(KafkaClients.class);
      Mockito.when(clients.template(name)).thenReturn(template);
      return clients;
   }

   @SuppressWarnings("unchecked")
   private static ProducerRecord<String, String> sentTo(KafkaTemplate<String, String> template) {
      ArgumentCaptor<ProducerRecord<String, String>> sent = ArgumentCaptor.forClass(ProducerRecord.class);
      Mockito.verify(template).send(sent.capture());
      return sent.getValue();
   }

   @Test
   void createsNoPublisherUnlessTheServiceChoosesKafka() {
      runner.run(context -> assertThat(context).doesNotHaveBean(OutboxPublisher.class));
      runner.withPropertyValues("ecom.outbox.client=sqs.orders")
          .run(context -> assertThat(context).doesNotHaveBean(OutboxPublisher.class));
   }

   @Test
   void publishesThroughTheNamedClientToTheAggregateTopicKeyedByAggregateIdWithTheMessageId() {
      KafkaTemplate<String, String> template = templateThatAcknowledges();
      UUID id = UUID.randomUUID();

      runner.withBean(KafkaClients.class, () -> clientsWith("core", template))
          .withPropertyValues("ecom.outbox.client=kafka.core")
          .run(context -> context.getBean(OutboxPublisher.class).publish(message(id)));

      ProducerRecord<String, String> record = sentTo(template);
      assertThat(record.topic()).isEqualTo("ecom.product");
      assertThat(record.key()).isEqualTo("p-1");
      assertThat(record.value()).isEqualTo("{\"aggregate\":\"product\"}");
      assertThat(new String(record.headers().lastHeader("message-id").value(), StandardCharsets.UTF_8))
          .isEqualTo(id.toString());
   }

   @Test
   void topicPrefixIsConfigurable() {
      KafkaTemplate<String, String> template = templateThatAcknowledges();

      runner.withBean(KafkaClients.class, () -> clientsWith("core", template))
          .withPropertyValues("ecom.outbox.client=kafka.core", "ecom.adapter.kafka.topic-prefix=shop.")
          .run(context -> context.getBean(OutboxPublisher.class).publish(message(UUID.randomUUID())));

      assertThat(sentTo(template).topic()).isEqualTo("shop.product");
   }

   @Test
   @SuppressWarnings("unchecked")
   void aFailedSendSurfacesSoTheOutboxRetries() {
      KafkaTemplate<String, String> template = Mockito.mock(KafkaTemplate.class);
      Mockito.when(template.send(Mockito.any(ProducerRecord.class)))
          .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker down")));

      runner.withBean(KafkaClients.class, () -> clientsWith("core", template))
          .withPropertyValues("ecom.outbox.client=kafka.core")
          .run(context -> assertThatThrownBy(() -> context.getBean(OutboxPublisher.class).publish(message(UUID.randomUUID())))
              .hasStackTraceContaining("broker down"));
   }

   @Test
   void aChoiceWithoutANameFailsStartup() {
      runner.withBean(KafkaClients.class, () -> Mockito.mock(KafkaClients.class))
          .withPropertyValues("ecom.outbox.client=kafka.")
          .run(context -> assertThat(context).hasFailed());
   }

   @Test
   void publisherDefinedByTheServiceWins() {
      OutboxPublisher own = m -> { };
      runner.withBean(OutboxPublisher.class, () -> own)
          .withBean(KafkaClients.class, () -> Mockito.mock(KafkaClients.class))
          .withPropertyValues("ecom.outbox.client=kafka.core")
          .run(context -> assertThat(context.getBean(OutboxPublisher.class)).isSameAs(own));
   }

   @Configuration(proxyBeanMethods = false)
   @Import({KafkaClientConfiguration.class, KafkaOutboxAdapterConfiguration.class})
   static class ServiceUsingTheOutbox {
   }

   @Test
   void theServiceNamesTheClientAndTheProfileSaysWhereItIs() {
      try (var context = new SpringApplicationBuilder(ServiceUsingTheOutbox.class)
          .web(WebApplicationType.NONE)
          .profiles("local")
          .properties("ecom.outbox.client=kafka.core")
          .run()) {
         assertThat(context.getBean(OutboxPublisher.class)).isNotNull();
      }
   }

   @Test
   void aNameTheProfileDoesNotDefineFailsStartupAndListsTheOnesItDoes() {
      assertThatThrownBy(() -> new SpringApplicationBuilder(ServiceUsingTheOutbox.class)
          .web(WebApplicationType.NONE)
          .profiles("local")
          .properties("ecom.outbox.client=kafka.nope")
          .run())
          .hasStackTraceContaining("No Kafka client named 'nope'")
          .hasStackTraceContaining("Available: [core]");
   }

   @Test
   void aServiceThatNamesNothingGetsNoPublisher() {
      try (var context = new SpringApplicationBuilder(ServiceUsingTheOutbox.class)
          .web(WebApplicationType.NONE)
          .profiles("local")
          .run()) {
         assertThat(context.getBeansOfType(OutboxPublisher.class)).isEmpty();
      }
   }
}
