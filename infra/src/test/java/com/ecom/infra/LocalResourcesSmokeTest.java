package com.ecom.infra;

import com.ecom.asset.AssetStorage;
import com.ecom.inbox.InboxListener;
import com.ecom.infra.adapter.asset.S3AssetStorageAdapterConfiguration;
import com.ecom.infra.adapter.inbox.KafkaInboxAdapterConfiguration;
import com.ecom.infra.adapter.inbox.RabbitInboxAdapterConfiguration;
import com.ecom.infra.adapter.outbox.KafkaOutboxAdapterConfiguration;
import com.ecom.infra.adapter.outbox.RabbitOutboxAdapterConfiguration;
import com.ecom.infra.client.dynamodb.DynamoDbClientConfiguration;
import com.ecom.infra.client.dynamodb.DynamoDbClients;
import com.ecom.infra.client.kafka.KafkaClientConfiguration;
import com.ecom.infra.client.mongodb.MongoClientConfiguration;
import com.ecom.infra.client.mongodb.MongoClients;
import com.ecom.infra.client.rabbit.RabbitClientConfiguration;
import com.ecom.infra.client.s3.S3ClientConfiguration;
import com.ecom.outbox.OutboxMessage;
import com.ecom.outbox.OutboxPublisher;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
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
 * Runs the real configuration against the containers from {@code bootstrap/local} (RabbitMQ on 5672, DynamoDB Local
 * on 8000, S3Mock on 9090, and Kafka on 9092 when started with its compose profile) with nothing but the {@code local} profile. Each
 * test is skipped when its container is not running, so the normal build never needs Docker. Start them with {@code bootstrap/local/local.ps1 up}.
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

   private static void requireLocal(int port, String name) {
      Assumptions.assumeTrue(answers(port),
          name + " is not running; start the local resources with bootstrap/local/local.ps1 up");
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
       KafkaClientConfiguration.class, RabbitClientConfiguration.class, DynamoDbClientConfiguration.class,
       MongoClientConfiguration.class,
       KafkaOutboxAdapterConfiguration.class, KafkaInboxAdapterConfiguration.class,
       RabbitOutboxAdapterConfiguration.class, RabbitInboxAdapterConfiguration.class,
       S3ClientConfiguration.class, S3AssetStorageAdapterConfiguration.class})
   static class Service {
   }

   private static ConfigurableApplicationContext startService(String client) {
      String group = "smoke-" + UUID.randomUUID();
      return new SpringApplicationBuilder(Service.class)
          .web(WebApplicationType.NONE)
          .profiles("local")
          .properties(
              "ecom.outbox.client=" + client,
              "ecom.inbox.client=" + client,
              "ecom.adapter.kafka.consumer.group-id=" + group,
              "ecom.adapter.rabbit.consumer.group=" + group)
          .run();
   }

   private static void publishAndExpectDelivery(ConfigurableApplicationContext context, String broker) throws Exception {
      String payload = "{\"aggregate\":\"" + AGGREGATE + "\",\"action\":\"created\",\"data\":{\"id\":\"1\"}}";
      context.getBean(OutboxPublisher.class).publish(new OutboxMessage() {
         public UUID id() { return UUID.randomUUID(); }
         public String aggregateType() { return AGGREGATE; }
         public String aggregateId() { return "1"; }
         public String payload() { return payload; }
         public Instant createdAt() { return Instant.now(); }
      });

      assertThat(DELIVERED.await(60, TimeUnit.SECONDS))
          .as("the inbox listener should receive the message published to " + broker)
          .isTrue();
      assertThat(RECEIVED).containsExactly(AGGREGATE + ".created|{\"id\":\"1\"}");
   }

   @Test
   void anOutboxMessageTravelsThroughRealKafkaIntoTheInboxListener() throws Exception {
      requireLocal(9092, "Kafka");

      try (ConfigurableApplicationContext context = startService("kafka.core")) {
         publishAndExpectDelivery(context, "Kafka");
      }
   }

   @Test
   void anOutboxMessageTravelsThroughRealRabbitMqIntoTheInboxListener() throws Exception {
      requireLocal(5672, "RabbitMQ");

      try (ConfigurableApplicationContext context = startService("rabbit.core")) {
         publishAndExpectDelivery(context, "RabbitMQ");
      }
   }

   @Test
   void theMongoDbClientReadsAndWritesAgainstTheLocalContainer() {
      requireLocal(27017, "MongoDB");

      try (ConfigurableApplicationContext context = startService("")) {
         MongoCollection<Document> collection = context.getBean(MongoClients.class).client("main")
             .getDatabase("smoke_" + UUID.randomUUID().toString().replace("-", "")).getCollection("items");
         try {
            collection.insertOne(new Document("_id", "1").append("name", "smoke"));

            Document item = collection.find(new Document("_id", "1")).first();

            assertThat(item).isNotNull();
            assertThat(item.getString("name")).isEqualTo("smoke");
         } finally {
            collection.drop();
         }
      }
   }

   @Test
   void theDynamoDbClientReadsAndWritesAgainstDynamoDbLocal() {
      requireLocal(8000, "DynamoDB Local");

      try (ConfigurableApplicationContext context = startService("")) {
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

   @Test
   void aFileStoredInS3MockCanBeReadByAnyoneAtItsPublicUrl() throws Exception {
      requireLocal(9090, "S3Mock");

      try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Service.class)
          .web(WebApplicationType.NONE)
          .profiles("local")
          .properties("ecom.asset.client=s3.assets")
          .run()) {
         AssetStorage storage = context.getBean(AssetStorage.class);
         String key = "images/smoke/" + UUID.randomUUID() + ".png";
         byte[] content = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};
         storage.put(key, content, "image/png");
         try {
            var response = java.net.http.HttpClient.newHttpClient().send(
                java.net.http.HttpRequest.newBuilder(java.net.URI.create(storage.publicUrl(key))).build(),
                java.net.http.HttpResponse.BodyHandlers.ofByteArray());

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo(content);
            assertThat(response.headers().firstValue("Content-Type")).contains("image/png");
         } finally {
            storage.delete(key);
         }
         var gone = java.net.http.HttpClient.newHttpClient().send(
             java.net.http.HttpRequest.newBuilder(java.net.URI.create(storage.publicUrl(key))).build(),
             java.net.http.HttpResponse.BodyHandlers.discarding());
         assertThat(gone.statusCode()).isEqualTo(404);
      }
   }
}
