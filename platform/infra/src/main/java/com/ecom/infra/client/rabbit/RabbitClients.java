package com.ecom.infra.client.rabbit;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The RabbitMQ clients of the active profile, by name. Each one is created the first time it is asked for, once, and
 * shared; nothing connects before that and no setting is checked before that.
 */
public class RabbitClients implements DisposableBean {

   private final Map<String, RabbitClientProperties.Broker> brokers;
   private final Environment environment;

   private final Map<String, CachingConnectionFactory> connectionFactories = new ConcurrentHashMap<>();
   private final Map<String, RabbitTemplate> templates = new ConcurrentHashMap<>();
   private final Map<String, RabbitAdmin> admins = new ConcurrentHashMap<>();

   public RabbitClients(Map<String, RabbitClientProperties.Broker> brokers, Environment environment) {
      this.brokers = brokers;
      this.environment = environment;
   }

   /**
    * @throws IllegalArgumentException if the active profile defines no RabbitMQ client with this name
    */
   public void requireDefined(String name) {
      broker(name);
   }

   public Set<String> names() {
      return new TreeSet<>(brokers.keySet());
   }

   /**
    * The connection shared by everything that uses this client. Publishes can wait for the broker's confirmation.
    */
   public ConnectionFactory connectionFactory(String name) {
      return connectionFactories.computeIfAbsent(name, n -> {
         RabbitClientProperties.Broker broker = broker(n);
         if (isBlank(broker.host())) {
            throw new IllegalStateException("ecom.client.rabbit." + n + ".host is not set for active profiles "
                + Arrays.toString(environment.getActiveProfiles())
                + ". Add it to ecom-infra-<profile>.yml or to the service's configuration");
         }
         CachingConnectionFactory factory = new CachingConnectionFactory(broker.host().trim(),
             broker.port() == null ? 5672 : broker.port());
         if (!isBlank(broker.username())) {
            factory.setUsername(broker.username());
         }
         if (!isBlank(broker.password())) {
            factory.setPassword(broker.password());
         }
         if (!isBlank(broker.virtualHost())) {
            factory.setVirtualHost(broker.virtualHost().trim());
         }
         factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
         return factory;
      });
   }

   public RabbitTemplate template(String name) {
      return templates.computeIfAbsent(name, n -> new RabbitTemplate(connectionFactory(n)));
   }

   /**
    * Declares exchanges, queues and bindings on this broker; declaring something that already exists is harmless.
    */
   public RabbitAdmin admin(String name) {
      return admins.computeIfAbsent(name, n -> new RabbitAdmin(connectionFactory(n)));
   }

   private RabbitClientProperties.Broker broker(String name) {
      RabbitClientProperties.Broker broker = brokers.get(name);
      if (broker == null) {
         throw new IllegalArgumentException("No RabbitMQ client named '" + name + "' for active profiles "
             + Arrays.toString(environment.getActiveProfiles()) + ". Available: " + names());
      }
      return broker;
   }

   private static boolean isBlank(String value) {
      return value == null || value.isBlank();
   }

   @Override
   public void destroy() {
      connectionFactories.values().forEach(CachingConnectionFactory::destroy);
   }
}
