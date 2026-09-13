package com.ecom.infra.client.dynamodb;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The DynamoDB clients of the active profile, by name. Each one is created the first time it is asked for, once, and
 * shared; nothing connects before that and no setting is checked before that.
 */
public class DynamoDbClients implements DisposableBean {

   private final Map<String, DynamoDbClientProperties.Account> accounts;
   private final Environment environment;

   private final Map<String, DynamoDbClient> clients = new ConcurrentHashMap<>();
   private final Map<String, DynamoDbEnhancedClient> enhancedClients = new ConcurrentHashMap<>();

   public DynamoDbClients(Map<String, DynamoDbClientProperties.Account> accounts, Environment environment) {
      this.accounts = accounts;
      this.environment = environment;
   }

   /**
    * @throws IllegalArgumentException if the active profile defines no DynamoDB client with this name
    */
   public void requireDefined(String name) {
      account(name);
   }

   public Set<String> names() {
      return new TreeSet<>(accounts.keySet());
   }

   public DynamoDbClient client(String name) {
      return clients.computeIfAbsent(name, this::build);
   }

   public DynamoDbEnhancedClient enhanced(String name) {
      return enhancedClients.computeIfAbsent(name,
          n -> DynamoDbEnhancedClient.builder().dynamoDbClient(client(n)).build());
   }

   private DynamoDbClient build(String name) {
      DynamoDbClientProperties.Account account = account(name);
      if (isBlank(account.region())) {
         throw new IllegalStateException("ecom.client.dynamodb." + name + ".region is not set for active profiles "
             + Arrays.toString(environment.getActiveProfiles())
             + ". Add it to ecom-infra-<profile>.yml or to the service's configuration");
      }
      DynamoDbClientBuilder builder = DynamoDbClient.builder()
          .region(Region.of(account.region().trim()))
          .credentialsProvider(credentials(name, account));
      if (!isBlank(account.endpoint())) {
         builder.endpointOverride(URI.create(account.endpoint().trim()));
      }
      return builder.build();
   }

   private static AwsCredentialsProvider credentials(String name, DynamoDbClientProperties.Account account) {
      boolean hasKey = !isBlank(account.accessKeyId());
      if (hasKey != !isBlank(account.secretAccessKey())) {
         throw new IllegalStateException(
             "ecom.client.dynamodb." + name + ".access-key-id and secret-access-key must be set together");
      }
      if (hasKey) {
         return StaticCredentialsProvider.create(
             AwsBasicCredentials.create(account.accessKeyId().trim(), account.secretAccessKey().trim()));
      }
      return DefaultCredentialsProvider.builder().build();
   }

   private DynamoDbClientProperties.Account account(String name) {
      DynamoDbClientProperties.Account account = accounts.get(name);
      if (account == null) {
         throw new IllegalArgumentException("No DynamoDB client named '" + name + "' for active profiles "
             + Arrays.toString(environment.getActiveProfiles()) + ". Available: " + names());
      }
      return account;
   }

   private static boolean isBlank(String value) {
      return value == null || value.isBlank();
   }

   @Override
   public void destroy() {
      clients.values().forEach(DynamoDbClient::close);
   }
}
