package com.ecom.infra.client.mongodb;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.Map;

/**
 * The MongoDB clients this profile knows, by name, under {@code ecom.client.mongodb.<name>}.
 *
 * @param mongodb the clients, keyed by name (for example {@code main})
 */
@ConfigurationProperties(prefix = "ecom.client")
public record MongoClientProperties(@DefaultValue Map<String, Server> mongodb) {

   /**
    * One client. The URI may carry credentials, so outside the local profile it comes from an environment variable.
    *
    * @param uri      connection string, for example {@code mongodb://localhost:27017}
    * @param database optional default database name; a service that names its own database ignores it
    */
   public record Server(String uri, String database) {
   }
}
