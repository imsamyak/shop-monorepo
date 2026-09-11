package com.ecom.infra.client.s3;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The S3 clients of the active profile, by name. Each one is created the first time it is asked for, once, and
 * shared; nothing connects before that and no setting is checked before that.
 */
public class S3Clients implements DisposableBean {

   private final Map<String, S3ClientProperties.Bucket> buckets;
   private final Environment environment;
   private final Map<String, S3Client> clients = new ConcurrentHashMap<>();

   public S3Clients(Map<String, S3ClientProperties.Bucket> buckets, Environment environment) {
      this.buckets = buckets;
      this.environment = environment;
   }

   /**
    * @throws IllegalArgumentException if the active profile defines no S3 client with this name
    */
   public void requireDefined(String name) {
      definition(name);
   }

   public Set<String> names() {
      return new TreeSet<>(buckets.keySet());
   }

   public S3ClientProperties.Bucket definition(String name) {
      S3ClientProperties.Bucket bucket = buckets.get(name);
      if (bucket == null) {
         throw new IllegalArgumentException("No S3 client named '" + name + "' for active profiles "
             + Arrays.toString(environment.getActiveProfiles()) + ". Available: " + names());
      }
      return bucket;
   }

   public S3Client client(String name) {
      return clients.computeIfAbsent(name, this::create);
   }

   private S3Client create(String name) {
      S3ClientProperties.Bucket bucket = definition(name);
      if (isBlank(bucket.bucket()) || isBlank(bucket.region())) {
         throw new IllegalStateException("ecom.client.s3." + name + ".bucket and .region must be set for active profiles "
             + Arrays.toString(environment.getActiveProfiles())
             + ". Add them to ecom-infra-<profile>.yml or to the service's configuration");
      }
      var builder = S3Client.builder()
          .region(Region.of(bucket.region().trim()))
          .credentialsProvider(credentials(bucket))
          .serviceConfiguration(S3Configuration.builder()
              .pathStyleAccessEnabled(Boolean.TRUE.equals(bucket.pathStyle()))
              .build());
      if (!isBlank(bucket.endpoint())) {
         builder.endpointOverride(URI.create(bucket.endpoint().trim()));
      }
      return builder.build();
   }

   private static AwsCredentialsProvider credentials(S3ClientProperties.Bucket bucket) {
      if (!isBlank(bucket.accessKeyId()) && !isBlank(bucket.secretAccessKey())) {
         return StaticCredentialsProvider.create(AwsBasicCredentials.create(bucket.accessKeyId(), bucket.secretAccessKey()));
      }
      return DefaultCredentialsProvider.builder().build();
   }

   private static boolean isBlank(String value) {
      return value == null || value.isBlank();
   }

   @Override
   public void destroy() {
      clients.values().forEach(S3Client::close);
   }
}
