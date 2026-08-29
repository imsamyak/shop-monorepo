package com.ecom.infra.client.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.Map;

/**
 * The Kafka clusters this profile knows, by name, under {@code ecom.client.kafka.<name>}.
 *
 * @param kafka the clusters, keyed by name (for example {@code core})
 */
@ConfigurationProperties(prefix = "ecom.client")
public record KafkaClientProperties(@DefaultValue Map<String, Cluster> kafka) {

   /**
    * One cluster. Logging in is part of the same settings: put {@code security.protocol}, {@code sasl.*} or
    * {@code ssl.*} under {@code properties}, with secrets coming from environment variables.
    *
    * @param bootstrapServers comma-separated {@code host:port} list
    * @param clientId         optional client id, shown in broker logs
    * @param properties       extra Kafka settings passed through as they are
    */
   public record Cluster(String bootstrapServers, String clientId, Map<String, String> properties) {

      public Cluster {
         properties = properties == null ? Map.of() : Map.copyOf(properties);
      }
   }
}
