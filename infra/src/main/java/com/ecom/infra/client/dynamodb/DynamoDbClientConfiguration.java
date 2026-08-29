package com.ecom.infra.client.dynamodb;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Makes the named DynamoDB clients of the active profile available as {@link DynamoDbClients}. The bean is lazy and
 * only exists when the AWS library is on the classpath; a bean defined by the service replaces ours.
 */
@AutoConfiguration
@ConditionalOnClass(DynamoDbClient.class)
@EnableConfigurationProperties(DynamoDbClientProperties.class)
public class DynamoDbClientConfiguration {

   @Bean
   @Lazy
   @ConditionalOnMissingBean
   DynamoDbClients dynamoDbClients(DynamoDbClientProperties properties, Environment environment) {
      return new DynamoDbClients(properties.dynamodb(), environment);
   }
}
