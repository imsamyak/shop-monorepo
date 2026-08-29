package com.ecom.infra.client.kafka;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Makes the named Kafka clients of the active profile available as {@link KafkaClients}. The bean is lazy and only
 * exists when spring-kafka is on the classpath; a bean defined by the service replaces ours.
 */
@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
@EnableConfigurationProperties(KafkaClientProperties.class)
public class KafkaClientConfiguration {

   @Bean
   @Lazy
   @ConditionalOnMissingBean
   KafkaClients kafkaClients(KafkaClientProperties properties, Environment environment) {
      return new KafkaClients(properties.kafka(), environment);
   }
}
