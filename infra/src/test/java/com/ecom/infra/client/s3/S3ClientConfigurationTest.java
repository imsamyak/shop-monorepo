package com.ecom.infra.client.s3;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class S3ClientConfigurationTest {

   private final ApplicationContextRunner runner = new ApplicationContextRunner()
       .withConfiguration(AutoConfigurations.of(S3ClientConfiguration.class));

   @Test
   void knowsTheBucketsTheProfileDefinesAndConnectsToNoneOfThemYet() {
      runner.withPropertyValues("ecom.client.s3.assets.bucket=a", "ecom.client.s3.assets.region=ap-south-1",
              "ecom.client.s3.backups.bucket=b", "ecom.client.s3.backups.region=ap-south-1")
          .run(context -> assertThat(context.getBean(S3Clients.class).names()).containsExactly("assets", "backups"));
   }

   @Test
   void buildsAClientForTheNamedBucketOnceAndSharesIt() {
      runner.withPropertyValues("ecom.client.s3.assets.bucket=shop-catalog-sk", "ecom.client.s3.assets.region=ap-south-1",
              "ecom.client.s3.assets.endpoint=http://localhost:9000", "ecom.client.s3.assets.path-style=true",
              "ecom.client.s3.assets.access-key-id=k", "ecom.client.s3.assets.secret-access-key=s")
          .run(context -> {
             S3Clients clients = context.getBean(S3Clients.class);
             assertThat(clients.client("assets")).isNotNull().isSameAs(clients.client("assets"));
             assertThat(clients.definition("assets").bucket()).isEqualTo("shop-catalog-sk");
          });
   }

   @Test
   void anUnknownNameListsTheOnesThatExist() {
      runner.withPropertyValues("ecom.client.s3.assets.bucket=a", "ecom.client.s3.assets.region=ap-south-1")
          .run(context -> assertThatThrownBy(() -> context.getBean(S3Clients.class).requireDefined("nope"))
              .hasMessageContaining("No S3 client named 'nope'")
              .hasMessageContaining("Available: [assets]"));
   }

   @Test
   void aBucketWithoutANameOrRegionFailsOnFirstUseNotAtStartup() {
      runner.withPropertyValues("ecom.client.s3.assets.region=ap-south-1").run(context -> {
         assertThat(context).hasNotFailed();
         assertThatThrownBy(() -> context.getBean(S3Clients.class).client("assets"))
             .hasMessageContaining("ecom.client.s3.assets.bucket and .region must be set");
      });
   }
}
