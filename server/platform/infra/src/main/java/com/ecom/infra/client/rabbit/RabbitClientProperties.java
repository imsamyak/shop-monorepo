package com.ecom.infra.client.rabbit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.Map;

/**
 * The RabbitMQ brokers this profile knows, by name, under {@code ecom.client.rabbit.<name>}.
 *
 * @param rabbit the brokers, keyed by name (for example {@code core})
 */
@ConfigurationProperties(prefix = "ecom.client")
public record RabbitClientProperties(@DefaultValue Map<String, Broker> rabbit) {

   /**
    * One broker. Passwords come from environment variables outside the local profile.
    *
    * @param host        broker host name
    * @param port        AMQP port, 5672 (5671 with {@code ssl}) when not set
    * @param username    login
    * @param password    password
    * @param virtualHost virtual host, {@code /} when not set
    * @param ssl         connect over TLS and check the broker's certificate (a hosted broker, {@code amqps://})
    */
   public record Broker(String host, Integer port, String username, String password, String virtualHost, Boolean ssl) {
   }
}
