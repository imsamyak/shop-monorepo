package com.ecom.infra;

import com.ecom.inbox.InboxListener;
import com.ecom.infra.adapter.inbox.KafkaInboxAdapterConfiguration;
import com.ecom.infra.adapter.outbox.KafkaOutboxAdapterConfiguration;
import com.ecom.infra.client.dynamodb.DynamoDbClientConfiguration;
import com.ecom.infra.client.dynamodb.DynamoDbClients;
import com.ecom.infra.client.kafka.KafkaClientConfiguration;
import com.ecom.outbox.OutboxMessage;
import com.ecom.outbox.OutboxPublisher;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the real configuration against the containers from {@code bootstrap/local} (Kafka on 9092, DynamoDB Local on
 * 8000) with nothing but the {@code local} profile. Skipped when they are not running, so the normal build never
 * needs Docker. Start them with {@code bootstrap/local/local.ps1 up}.
 */
class LocalResourcesSmokeTest {

   private static boolean answers(int port) {
      try (Socket socket = new Socket()) {
         socket.connect(new InetSocketAddress("127.0.0.1", port), 500);
         return true;
      } catch (Exception e) {
         return false;
      }
   }

   private static void requireLocalResources() {
      Assumptions.assumeTrue(answers(9092) && answers(8000),
          "Local resources are not running; start them with bootstrap/local/local.ps1 up");
   }

   static final CopyOnWriteArrayList<String> RECEIVED = new CopyOnWriteArrayList<>();
   static final CountDownLatch DELIVERED = new CountDownLatch(1);
   static final String AGGREGATE = "smoke" + UUID.randomUUID().toString().substring(0, 8);

   @Configuration(proxyBeanMethods = false)
   static class ServiceWithOneInboxUseCase {

      @Bean
      InboxListener inboxListener() {
         return new InboxListener() {
            @Override
            public void listen(String messageId, String signature, String payloadJson) {
               RECEIVED.add(signature + "|" + payloadJson);
               DELIVERED.countDown();
            }

            @Override
            public Set<String> subscribedAggregates() {
               return Set.of(AGGREGATE);
            }
         };
      }
   }

   @Configuration(proxyBeanMethods = false)
   @Import({ServiceWithOneInboxUseCase.class,
       KafkaClientConfiguration.class, DynamoDbClientConfiguration.class,
       KafkaOutboxAdapterConfiguration.class, KafkaInboxAdapterConfiguration.class})
   static class Service {
   }

   private static ConfigurableApplicationContext startService() {
      return new SpringApplicationBuilder(Service.class)
          .web(WebApplicationType.NONE)
          .profiles("local")
          .properties(
              "ecom.outbox.client=kafka.core",
              "ecom.inbox.client=kafka.core",
              "ecom.adapter.kafka.consumer.group-id=smoke-" + UUID.randomUUID())
          .run();
   }

   @Test
   void anOutboxMessageTravelsThroughRealKafkaIntoTheInboxListener() throws Exception {
      requireLocalResources();

      try (ConfigurableApplicationContext context = startService()) {
         String payload = "{\"aggregate\":\"" + AGGREGATE + "\",\"action\":\"created\",\"data\":{\"id\":\"1\"}}";
         context.getBean(OutboxPublisher.class).publish(new OutboxMessage() {
            public UUID id() { return UUID.randomUUID(); }
            public String aggregateType() { return AGGREGATE; }
            public String aggregateId() { return "1"; }
            public String payload() { return payload; }
            public Instant createdAt() { return Instant.now(); }
         });

         assertThat(DELIVERED.await(60, TimeUnit.SECONDS))
             .as("the inbox listener should receive the message published to Kafka")
             .isTrue();
         assertThat(RECEIVED).containsExactly(AGGREGATE + ".created|{\"id\":\"1\"}");
      }
   }

   @Test
   void theDynamoDbClientReadsAndWritesAgainstDynamoDbLocal() {
      requireLocalResources();

      try (ConfigurableApplicationContext context = startService()) {
         DynamoDbClient dynamo = context.getBean(DynamoDbClients.class).client("main");
         String table = "smoke_" + UUID.randomUUID().toString().replace("-", "");

         dynamo.createTable(request -> request
             .tableName(table)
             .billingMode(BillingMode.PAY_PER_REQUEST)
             .attributeDefinitions(AttributeDefinition.builder()
                 .attributeName("id").attributeType(ScalarAttributeType.S).build())
             .keySchema(KeySchemaElement.builder().attributeName("id").keyType(KeyType.HASH).build()));
         try {
            dynamo.putItem(request -> request.tableName(table)
                .item(Map.of("id", AttributeValue.fromS("1"), "name", AttributeValue.fromS("smoke"))));

            Map<String, AttributeValue> item = dynamo.getItem(request -> request.tableName(table)
                .key(Map.of("id", AttributeValue.fromS("1")))).item();

            assertThat(item.get("name").s()).isEqualTo("smoke");
         } finally {
            dynamo.deleteTable(request -> request.tableName(table));
         }
      }
   }
}
