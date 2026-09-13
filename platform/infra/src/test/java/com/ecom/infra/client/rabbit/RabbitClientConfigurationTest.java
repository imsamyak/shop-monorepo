package com.ecom.infra.client.rabbit;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RabbitClientConfigurationTest {

   private final ApplicationContextRunner runner = new ApplicationContextRunner()
       .withConfiguration(AutoConfigurations.of(RabbitClientConfiguration.class));

   @Test
   void knowsTheBrokersTheProfileDefinesAndConnectsToNoneOfThemYet() {
      runner.withPropertyValues("ecom.client.rabbit.core.host=localhost", "ecom.client.rabbit.slow.host=other")
          .run(context -> assertThat(context.getBean(RabbitClients.class).names()).containsExactly("core", "slow"));
   }

   @Test
   void buildsTheConnectionFromTheNamedSettingsWithDefaultsForPortAndVirtualHost() {
      runner.withPropertyValues("ecom.client.rabbit.core.host=broker.internal",
              "ecom.client.rabbit.core.username=app", "ecom.client.rabbit.core.password=secret")
          .run(context -> {
             CachingConnectionFactory factory =
                 (CachingConnectionFactory) context.getBean(RabbitClients.class).connectionFactory("core");
             assertThat(factory.getHost()).isEqualTo("broker.internal");
             assertThat(factory.getPort()).isEqualTo(5672);
             assertThat(factory.getVirtualHost()).isEqualTo("/");
             assertThat(factory.getUsername()).isEqualTo("app");
             assertThat(context.getBean(RabbitClients.class).connectionFactory("core")).isSameAs(factory);
          });
   }

   @Test
   void anUnknownNameListsTheOnesThatExist() {
      runner.withPropertyValues("ecom.client.rabbit.core.host=localhost")
          .run(context -> assertThatThrownBy(() -> context.getBean(RabbitClients.class).requireDefined("nope"))
              .hasMessageContaining("No RabbitMQ client named 'nope'")
              .hasMessageContaining("Available: [core]"));
   }

   @Test
   void aBrokerWithoutAHostFailsOnFirstUseNotAtStartup() {
      runner.withPropertyValues("ecom.client.rabbit.core.port=5672").run(context -> {
         assertThat(context).hasNotFailed();
         assertThatThrownBy(() -> context.getBean(RabbitClients.class).connectionFactory("core"))
             .hasMessageContaining("ecom.client.rabbit.core.host is not set");
      });
   }
}
