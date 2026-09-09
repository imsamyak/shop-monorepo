package com.ecom.infra.client.mongodb;

import com.mongodb.client.MongoClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The MongoDB clients of the active profile, by name. Each one is created the first time it is asked for, once, and
 * shared; nothing connects before that and no setting is checked before that.
 */
public class MongoClients implements DisposableBean {

   private final Map<String, MongoClientProperties.Server> servers;
   private final Environment environment;

   private final Map<String, MongoClient> clients = new ConcurrentHashMap<>();

   public MongoClients(Map<String, MongoClientProperties.Server> servers, Environment environment) {
      this.servers = servers;
      this.environment = environment;
   }

   /**
    * @throws IllegalArgumentException if the active profile defines no MongoDB client with this name
    */
   public void requireDefined(String name) {
      server(name);
   }

   public Set<String> names() {
      return new TreeSet<>(servers.keySet());
   }

   public MongoClient client(String name) {
      return clients.computeIfAbsent(name, this::build);
   }

   /**
    * The default database configured for this client, if any.
    */
   public Optional<String> database(String name) {
      return Optional.ofNullable(server(name).database()).filter(database -> !isBlank(database)).map(String::trim);
   }

   private MongoClient build(String name) {
      MongoClientProperties.Server server = server(name);
      if (isBlank(server.uri())) {
         throw new IllegalStateException("ecom.client.mongodb." + name + ".uri is not set for active profiles "
             + Arrays.toString(environment.getActiveProfiles())
             + ". Add it to ecom-infra-<profile>.yml or to the service's configuration");
      }
      return com.mongodb.client.MongoClients.create(server.uri().trim());
   }

   private MongoClientProperties.Server server(String name) {
      MongoClientProperties.Server server = servers.get(name);
      if (server == null) {
         throw new IllegalArgumentException("No MongoDB client named '" + name + "' for active profiles "
             + Arrays.toString(environment.getActiveProfiles()) + ". Available: " + names());
      }
      return server;
   }

   private static boolean isBlank(String value) {
      return value == null || value.isBlank();
   }

   @Override
   public void destroy() {
      clients.values().forEach(MongoClient::close);
   }
}
