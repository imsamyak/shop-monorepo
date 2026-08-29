package com.ecom.infra.client;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;

/**
 * Gives this module its own per-profile properties: for every active profile it loads
 * {@code classpath:ecom-infra-<profile>.yml}, if present, as the lowest-priority property source.
 * The values are ordinary Spring properties (for example {@code ecom.client.kafka.core.bootstrap-servers}), so a service's
 * {@code application.yml}, environment variables or command-line arguments override them in the usual way.
 * When several profiles are active, the last one listed wins.
 */
public class ProfilePropertiesEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

   private static final String FILE_PREFIX = "ecom-infra-";

   private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

   @Override
   public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
      MutablePropertySources sources = environment.getPropertySources();
      String[] profiles = environment.getActiveProfiles();

      for (int i = profiles.length - 1; i >= 0; i--) {
         Resource resource = new ClassPathResource(FILE_PREFIX + profiles[i] + ".yml");
         String name = "ecomInfra[" + resource.getFilename() + "]";
         if (!resource.exists() || sources.contains(name)) {
            continue;
         }
         try {
            loader.load(name, resource).forEach(sources::addLast);
         } catch (IOException e) {
            throw new IllegalStateException("Cannot read " + resource.getFilename(), e);
         }
      }
   }

   /**
    * Runs after Spring Boot has read the service's own configuration, so every active profile is known.
    */
   @Override
   public int getOrder() {
      return Ordered.LOWEST_PRECEDENCE;
   }
}
