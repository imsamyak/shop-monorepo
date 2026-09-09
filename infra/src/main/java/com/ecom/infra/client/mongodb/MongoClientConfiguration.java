package com.ecom.infra.client.mongodb;

import com.mongodb.client.MongoClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;

/**
 * Makes the named MongoDB clients of the active profile available as {@link MongoClients}. The bean is lazy and
 * only exists when the MongoDB driver is on the classpath; a bean defined by the service replaces ours.
 */
@AutoConfiguration
@ConditionalOnClass(MongoClient.class)
@EnableConfigurationProperties(MongoClientProperties.class)
public class MongoClientConfiguration {

   @Bean
   @Lazy
   @ConditionalOnMissingBean
   MongoClients mongoClients(MongoClientProperties properties, Environment environment) {
      return new MongoClients(properties.mongodb(), environment);
   }
}
