package com.ecom.asset;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Gives a service an {@link ImageUploader} once it has an {@link AssetStorage}. Without storage there is no uploader,
 * and the service fails to start instead of failing at the first upload.
 */
@AutoConfiguration(afterName = "com.ecom.infra.adapter.asset.S3AssetStorageAdapterConfiguration")
@EnableConfigurationProperties(AssetProperties.class)
public class AssetAutoConfiguration {

   @Bean
   @ConditionalOnBean(AssetStorage.class)
   @ConditionalOnMissingBean
   ImageUploader imageUploader(AssetStorage storage, AssetProperties properties) {
      return new ImageUploader(storage, properties);
   }
}
