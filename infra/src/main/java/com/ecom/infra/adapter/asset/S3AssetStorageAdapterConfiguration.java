package com.ecom.infra.adapter.asset;

import com.ecom.asset.AssetStorage;
import com.ecom.infra.adapter.ClientReference;
import com.ecom.infra.client.s3.S3ClientProperties;
import com.ecom.infra.client.s3.S3Clients;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Keeps assets in the S3 bucket the service named with {@code ecom.asset.client: s3.<name>}. Files are stored without
 * any ACL: who may read them is the bucket's business (a bucket policy that allows public {@code s3:GetObject}), so
 * this code never needs permission to change access. A file's key is random and never reused, so it is cached for a
 * year. A storage bean defined by the service replaces this one.
 */
@AutoConfiguration(afterName = "com.ecom.infra.client.s3.S3ClientConfiguration")
@ConditionalOnClass({AssetStorage.class, S3Client.class})
@ConditionalOnExpression("'${ecom.asset.client:}'.toLowerCase().startsWith('s3.')")
public class S3AssetStorageAdapterConfiguration {

   static final String CACHE_CONTROL = "public, max-age=31536000, immutable";

   @Bean
   @ConditionalOnMissingBean(AssetStorage.class)
   AssetStorage s3AssetStorage(S3Clients clients, Environment environment) {
      String name = ClientReference.of(environment, "asset").name();
      clients.requireDefined(name);
      return new S3AssetStorage(clients, name);
   }

   static final class S3AssetStorage implements AssetStorage {

      private final S3Clients clients;
      private final String name;

      S3AssetStorage(S3Clients clients, String name) {
         this.clients = clients;
         this.name = name;
      }

      @Override
      public void put(String key, byte[] content, String contentType) {
         clients.client(name).putObject(PutObjectRequest.builder()
             .bucket(clients.definition(name).bucket())
             .key(key)
             .contentType(contentType)
             .cacheControl(CACHE_CONTROL)
             .build(), RequestBody.fromBytes(content));
      }

      @Override
      public void delete(String key) {
         clients.client(name).deleteObject(DeleteObjectRequest.builder()
             .bucket(clients.definition(name).bucket())
             .key(key)
             .build());
      }

      @Override
      public String publicUrl(String key) {
         S3ClientProperties.Bucket bucket = clients.definition(name);
         return baseUrl(bucket) + "/" + key;
      }

      private static String baseUrl(S3ClientProperties.Bucket bucket) {
         if (hasText(bucket.publicBaseUrl())) {
            return trimSlash(bucket.publicBaseUrl());
         }
         if (hasText(bucket.endpoint())) {
            return trimSlash(bucket.endpoint()) + "/" + bucket.bucket();
         }
         return "https://" + bucket.bucket() + ".s3." + bucket.region() + ".amazonaws.com";
      }

      private static boolean hasText(String value) {
         return value != null && !value.isBlank();
      }

      private static String trimSlash(String value) {
         String trimmed = value.trim();
         return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
      }
   }
}
