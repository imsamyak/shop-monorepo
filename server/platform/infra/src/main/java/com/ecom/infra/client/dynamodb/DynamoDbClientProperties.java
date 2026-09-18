package com.ecom.infra.client.dynamodb;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.Map;

/**
 * The DynamoDB clients this profile knows, by name, under {@code ecom.client.dynamodb.<name>}.
 *
 * @param dynamodb the clients, keyed by name (for example {@code main})
 */
@ConfigurationProperties(prefix = "ecom.client")
public record DynamoDbClientProperties(@DefaultValue Map<String, Account> dynamodb) {

   /**
    * One client. Credentials come from the AWS default chain (environment, shared profile, IAM role). The static
    * keys exist only for local containers that insist on some key; do not use them for real AWS accounts.
    *
    * @param region          AWS region, for example {@code ap-south-1}
    * @param endpoint        optional custom endpoint, for example {@code http://localhost:8000}
    * @param accessKeyId     optional static key, local use only
    * @param secretAccessKey optional static secret, local use only
    */
   public record Account(String region, String endpoint, String accessKeyId, String secretAccessKey) {
   }
}
