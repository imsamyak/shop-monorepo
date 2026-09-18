package com.ecom.infra.client.kafka;

import com.ecom.infra.client.ProfilePropertiesEnvironmentPostProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KafkaClientConfigurationTest {

   private final ApplicationContextRunner runner = new ApplicationContextRunner()
       .withConfiguration(AutoConfigurations.of(KafkaClientConfiguration.class));

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
         assertThat(context.getBean(KafkaClients.class).names()).isEmpty();
      });
   }

   @Test
   void knowsEveryNamedClusterAndCreatesEachOnlyOnFirstUse() {
      runner.withPropertyValues(
              "ecom.client.kafka.core.bootstrap-servers=localhost:9092",
              "ecom.client.kafka.core.client-id=test",
              "ecom.client.kafka.audit.bootstrap-servers=audit:9093")
          .run(context -> {
             KafkaClients clients = context.getBean(KafkaClients.class);
             assertThat(clients.names()).containsExactly("audit", "core");

             KafkaTemplate<String, String> core = clients.template("core");
             assertThat(clients.template("core")).isSameAs(core);
             assertThat(core.getProducerFactory().getConfigurationProperties())
                 .containsEntry("bootstrap.servers", "localhost:9092")
                 .containsEntry("client.id", "test")
                 .containsEntry("acks", "all")
                 .containsEntry("enable.idempotence", true);
             assertThat(clients.template("audit").getProducerFactory().getConfigurationProperties())
                 .containsEntry("bootstrap.servers", "audit:9093");
             assertThat(clients.consumerFactory("core").getConfigurationProperties())
                 .containsEntry("bootstrap.servers", "localhost:9092")
                 .containsEntry("enable.auto.commit", false)
                 .containsEntry("auto.offset.reset", "earliest");
          });
   }

   @Test
   void loginSettingsArePassedThroughToTheClient() {
      runner.withPropertyValues(
              "ecom.client.kafka.audit.bootstrap-servers=audit:9093",
              "ecom.client.kafka.audit.properties.security.protocol=SASL_SSL",
              "ecom.client.kafka.audit.properties.sasl.mechanism=SCRAM-SHA-512")
          .run(context -> assertThat(context.getBean(KafkaClients.class).template("audit")
              .getProducerFactory().getConfigurationProperties())
              .containsEntry("security.protocol", "SASL_SSL")
              .containsEntry("sasl.mechanism", "SCRAM-SHA-512"));
   }

   @Test
   void anUnknownNameFailsAndListsTheAvailableOnes() {
      runner.withPropertyValues("ecom.client.kafka.core.bootstrap-servers=localhost:9092")
          .run(context -> assertThatThrownBy(() -> context.getBean(KafkaClients.class).template("cor"))
              .hasMessageContaining("No Kafka client named 'cor'")
              .hasMessageContaining("Available: [core]"));
   }

   @Test
   void aClusterWithoutServersFailsOnFirstUseWithAClearMessage() {
      runner.withPropertyValues("ecom.client.kafka.core.client-id=only-an-id")
          .run(context -> assertThatThrownBy(() -> context.getBean(KafkaClients.class).template("core"))
              .hasStackTraceContaining("ecom.client.kafka.core.bootstrap-servers is not set for active profiles []"));
   }

   @Test
   void localProfileDefinesTheCoreCluster() {
      onProfile("local").run(context -> {
         KafkaClients clients = context.getBean(KafkaClients.class);
         assertThat(clients.names()).containsExactly("core");
         assertThat(clients.template("core").getProducerFactory().getConfigurationProperties())
             .containsEntry("bootstrap.servers", "localhost:9092");
      });
   }

   @Test
   void explicitPropertyOverridesTheProfileEntry() {
      onProfile("local").withPropertyValues("ecom.client.kafka.core.bootstrap-servers=other:9093")
          .run(context -> assertThat(context.getBean(KafkaClients.class).template("core")
              .getProducerFactory().getConfigurationProperties())
              .containsEntry("bootstrap.servers", "other:9093"));
   }

   @Test
   void aProfileWithoutEntriesKnowsNoClientAndNamesItselfInTheError() {
      onProfile("staging").run(context -> assertThatThrownBy(() -> context.getBean(KafkaClients.class).template("core"))
          .hasMessageContaining("active profiles [staging]")
          .hasMessageContaining("Available: []"));
   }

   @Test
   void registryDefinedByTheServiceWins() {
      KafkaClients own = new KafkaClients(java.util.Map.of(), null);
      runner.withBean("ownKafkaClients", KafkaClients.class, () -> own)
          .run(context -> assertThat(context.getBean(KafkaClients.class)).isSameAs(own));
   }

   @Configuration(proxyBeanMethods = false)
   @Import(KafkaClientConfiguration.class)
   static class App {
   }

   @Test
   void postProcessorIsRegisteredAndAppliedByARealApplicationStart() {
      try (var context = new SpringApplicationBuilder(App.class)
          .web(WebApplicationType.NONE)
          .profiles("local")
          .run()) {
         assertThat(context.getBean(KafkaClients.class).names()).containsExactly("core");
      }
   }
}
