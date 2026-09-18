package com.ecom.infra.client.rabbit;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;

/**
 * Makes the named RabbitMQ clients of the active profile available as {@link RabbitClients}. The bean is lazy and
 * only exists when spring-rabbit is on the classpath; a bean defined by the service replaces ours.
 */
@AutoConfiguration
@ConditionalOnClass(RabbitTemplate.class)
@EnableConfigurationProperties(RabbitClientProperties.class)
public class RabbitClientConfiguration {

   @Bean
   @Lazy
   @ConditionalOnMissingBean
   RabbitClients rabbitClients(RabbitClientProperties properties, Environment environment) {
      return new RabbitClients(properties.rabbit(), environment);
   }
}
