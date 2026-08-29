package com.ecom.infra.client.dynamodb;

import com.ecom.infra.client.ProfilePropertiesEnvironmentPostProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DynamoDbClientConfigurationTest {

   private final ApplicationContextRunner runner = new ApplicationContextRunner()
       .withConfiguration(AutoConfigurations.of(DynamoDbClientConfiguration.class));

   /**
    * The runner does not run environment post-processors, so apply the module's one by hand for the given profile.
    */
   private ApplicationContextRunner onProfile(String profile) {
      return runner.withInitializer(context -> {
         context.getEnvironment().setActiveProfiles(profile);
         new ProfilePropertiesEnvironmentPostProcessor().postProcessEnvironment(context.getEnvironment(), null);
      });
   }

   @Test
   void startsWithoutAnySettingAndKnowsNoClient() {
      runner.run(context -> {
         assertThat(context).hasNotFailed();
         assertThat(context.getBean(DynamoDbClients.class).names()).isEmpty();
      });
   }

   @Test
   void buildsEachNamedClientOnFirstUseAndSharesIt() {
      runner.withPropertyValues(
              "ecom.client.dynamodb.main.region=ap-south-1",
              "ecom.client.dynamodb.main.endpoint=http://localhost:8000",
              "ecom.client.dynamodb.main.access-key-id=local",
              "ecom.client.dynamodb.main.secret-access-key=local",
              "ecom.client.dynamodb.reports.region=eu-west-1",
              "ecom.client.dynamodb.reports.endpoint=http://localhost:8001",
              "ecom.client.dynamodb.reports.access-key-id=local",
              "ecom.client.dynamodb.reports.secret-access-key=local")
          .run(context -> {
             DynamoDbClients clients = context.getBean(DynamoDbClients.class);
             assertThat(clients.names()).containsExactly("main", "reports");

             DynamoDbClient main = clients.client("main");
             assertThat(clients.client("main")).isSameAs(main);
             assertThat(clients.client("reports")).isNotSameAs(main);
             assertThat(clients.enhanced("main")).isNotNull();
          });
   }

   @Test
   void anUnknownNameFailsAndListsTheAvailableOnes() {
      runner.withPropertyValues("ecom.client.dynamodb.main.region=ap-south-1")
          .run(context -> assertThatThrownBy(() -> context.getBean(DynamoDbClients.class).client("mian"))
              .hasMessageContaining("No DynamoDB client named 'mian'")
              .hasMessageContaining("Available: [main]"));
   }

   @Test
   void aClientWithoutRegionFailsOnFirstUse() {
      runner.withPropertyValues("ecom.client.dynamodb.main.endpoint=http://localhost:8000")
          .run(context -> assertThatThrownBy(() -> context.getBean(DynamoDbClients.class).client("main"))
              .hasStackTraceContaining("ecom.client.dynamodb.main.region is not set for active profiles []"));
   }

   @Test
   void rejectsHalfAStaticKeyPair() {
      runner.withPropertyValues(
              "ecom.client.dynamodb.main.region=ap-south-1",
              "ecom.client.dynamodb.main.access-key-id=only-key")
          .run(context -> assertThatThrownBy(() -> context.getBean(DynamoDbClients.class).client("main"))
              .hasStackTraceContaining("must be set together"));
   }

   @Test
   void localProfileDefinesTheMainClient() {
      onProfile("local").run(context -> {
         DynamoDbClients clients = context.getBean(DynamoDbClients.class);
         assertThat(clients.names()).containsExactly("main");
         assertThat(clients.enhanced("main")).isNotNull();
      });
   }

   @Test
   void aProfileWithoutEntriesKnowsNoClientAndNamesItselfInTheError() {
      onProfile("staging").run(context -> assertThatThrownBy(() -> context.getBean(DynamoDbClients.class).client("main"))
          .hasMessageContaining("active profiles [staging]")
          .hasMessageContaining("Available: []"));
   }
}
