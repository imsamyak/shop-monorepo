package com.ecom.infra.client.s3;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Makes the named S3 clients of the active profile available as {@link S3Clients}. The bean is lazy and only exists
 * when the AWS S3 SDK is on the classpath; a bean defined by the service replaces ours.
 */
@AutoConfiguration
@ConditionalOnClass(S3Client.class)
@EnableConfigurationProperties(S3ClientProperties.class)
public class S3ClientConfiguration {

   @Bean
   @Lazy
   @ConditionalOnMissingBean
   S3Clients s3Clients(S3ClientProperties properties, Environment environment) {
      return new S3Clients(properties.s3(), environment);
   }
}
