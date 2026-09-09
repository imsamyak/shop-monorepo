package com.ecom.infra.client.mongodb;

import com.mongodb.client.MongoClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MongoClientConfigurationTest {

   private final ApplicationContextRunner runner = new ApplicationContextRunner()
       .withConfiguration(AutoConfigurations.of(MongoClientConfiguration.class));

   @Test
   void knowsTheServersTheProfileDefinesAndConnectsToNoneOfThemYet() {
      runner.withPropertyValues("ecom.client.mongodb.main.uri=mongodb://localhost:27017",
              "ecom.client.mongodb.archive.uri=mongodb://other:27017")
          .run(context -> assertThat(context.getBean(MongoClients.class).names()).containsExactly("archive", "main"));
   }

   @Test
   void createsOneSharedClientPerNameFromTheUri() {
      runner.withPropertyValues("ecom.client.mongodb.main.uri=mongodb://db.internal:27018")
          .run(context -> {
             MongoClients clients = context.getBean(MongoClients.class);
             MongoClient client = clients.client("main");
             assertThat(client.getClusterDescription().getClusterSettings().getHosts())
                 .extracting(Object::toString).containsExactly("db.internal:27018");
             assertThat(clients.client("main")).isSameAs(client);
          });
   }

   @Test
   void theDatabaseIsOptional() {
      runner.withPropertyValues("ecom.client.mongodb.main.uri=mongodb://localhost:27017",
              "ecom.client.mongodb.main.database=shop", "ecom.client.mongodb.plain.uri=mongodb://localhost:27017")
          .run(context -> {
             MongoClients clients = context.getBean(MongoClients.class);
             assertThat(clients.database("main")).contains("shop");
             assertThat(clients.database("plain")).isEmpty();
          });
   }

   @Test
   void anUnknownNameListsTheOnesThatExist() {
      runner.withPropertyValues("ecom.client.mongodb.main.uri=mongodb://localhost:27017")
          .run(context -> assertThatThrownBy(() -> context.getBean(MongoClients.class).requireDefined("nope"))
              .hasMessageContaining("No MongoDB client named 'nope'")
              .hasMessageContaining("Available: [main]"));
   }

   @Test
   void aServerWithoutAUriFailsOnFirstUseNotAtStartup() {
      runner.withPropertyValues("ecom.client.mongodb.main.database=shop").run(context -> {
         assertThat(context).hasNotFailed();
         assertThatThrownBy(() -> context.getBean(MongoClients.class).client("main"))
             .hasMessageContaining("ecom.client.mongodb.main.uri is not set");
      });
   }
}
