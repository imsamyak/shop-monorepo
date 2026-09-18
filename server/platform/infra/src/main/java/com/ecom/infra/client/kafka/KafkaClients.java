package com.ecom.infra.client.kafka;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Kafka clients of the active profile, by name. Each one is created the first time it is asked for, once, and
 * shared; nothing connects before that and no setting is checked before that.
 */
public class KafkaClients implements DisposableBean {

   private final Map<String, KafkaClientProperties.Cluster> clusters;
   private final Environment environment;

   private final Map<String, DefaultKafkaProducerFactory<String, String>> producerFactories = new ConcurrentHashMap<>();
   private final Map<String, KafkaTemplate<String, String>> templates = new ConcurrentHashMap<>();
   private final Map<String, ConsumerFactory<String, String>> consumerFactories = new ConcurrentHashMap<>();

   public KafkaClients(Map<String, KafkaClientProperties.Cluster> clusters, Environment environment) {
      this.clusters = clusters;
      this.environment = environment;
   }

   /**
    * @throws IllegalArgumentException if the active profile defines no Kafka client with this name
    */
   public void requireDefined(String name) {
      cluster(name);
   }

   public Set<String> names() {
      return new TreeSet<>(clusters.keySet());
   }

   /**
    * A template whose producers wait for all in-sync replicas and do not duplicate on retry.
    */
   public KafkaTemplate<String, String> template(String name) {
      return templates.computeIfAbsent(name, n -> new KafkaTemplate<>(producerFactory(n)));
   }

   /**
    * A factory of consumers that commit offsets only when told to, starting from the earliest offset for a new group.
    */
   public ConsumerFactory<String, String> consumerFactory(String name) {
      return consumerFactories.computeIfAbsent(name, n -> {
         Map<String, Object> config = baseConfig(n);
         config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
         config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
         config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
         config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
         return new DefaultKafkaConsumerFactory<>(config);
      });
   }

   private DefaultKafkaProducerFactory<String, String> producerFactory(String name) {
      Map<String, Object> config = baseConfig(name);
      config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
      config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
      config.put(ProducerConfig.ACKS_CONFIG, "all");
      config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
      DefaultKafkaProducerFactory<String, String> factory = new DefaultKafkaProducerFactory<>(config);
      producerFactories.put(name, factory);
      return factory;
   }

   private Map<String, Object> baseConfig(String name) {
      KafkaClientProperties.Cluster cluster = cluster(name);
      if (isBlank(cluster.bootstrapServers())) {
         throw new IllegalStateException("ecom.client.kafka." + name + ".bootstrap-servers is not set for active profiles "
             + Arrays.toString(environment.getActiveProfiles())
             + ". Add it to ecom-infra-<profile>.yml or to the service's configuration");
      }
      Map<String, Object> config = new HashMap<>(cluster.properties());
      config.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, cluster.bootstrapServers().trim());
      if (!isBlank(cluster.clientId())) {
         config.put(CommonClientConfigs.CLIENT_ID_CONFIG, cluster.clientId().trim());
      }
      return config;
   }

   private KafkaClientProperties.Cluster cluster(String name) {
      KafkaClientProperties.Cluster cluster = clusters.get(name);
      if (cluster == null) {
         throw new IllegalArgumentException("No Kafka client named '" + name + "' for active profiles "
             + Arrays.toString(environment.getActiveProfiles()) + ". Available: " + names());
      }
      return cluster;
   }

   private static boolean isBlank(String value) {
      return value == null || value.isBlank();
   }

   @Override
   public void destroy() {
      producerFactories.values().forEach(DefaultKafkaProducerFactory::destroy);
   }
}
