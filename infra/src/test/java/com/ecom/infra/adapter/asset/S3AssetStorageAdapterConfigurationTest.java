package com.ecom.infra.adapter.asset;

import com.ecom.asset.AssetStorage;
import com.ecom.infra.client.s3.S3ClientProperties;
import com.ecom.infra.client.s3.S3Clients;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.assertj.core.api.Assertions.assertThat;

class S3AssetStorageAdapterConfigurationTest {

   private final ApplicationContextRunner runner = new ApplicationContextRunner()
       .withConfiguration(AutoConfigurations.of(S3AssetStorageAdapterConfiguration.class));

   private static S3Clients clientsFor(S3Client s3, S3ClientProperties.Bucket bucket) {
      S3Clients clients = Mockito.mock(S3Clients.class);
      Mockito.when(clients.client("assets")).thenReturn(s3);
      Mockito.when(clients.definition("assets")).thenReturn(bucket);
      return clients;
   }

   private static S3ClientProperties.Bucket aws() {
      return new S3ClientProperties.Bucket("shop-catalog-sk", "ap-south-1", null, null, null, null, null);
   }

   @Test
   void createsNoStorageUnlessTheServiceChoosesS3() {
      runner.run(context -> assertThat(context).doesNotHaveBean(AssetStorage.class));
      runner.withPropertyValues("ecom.asset.client=gcs.assets")
          .run(context -> assertThat(context).doesNotHaveBean(AssetStorage.class));
   }

   @Test
   void putsTheFileWithItsTypeAndALongCacheAndNoAcl() {
      S3Client s3 = Mockito.mock(S3Client.class);

      runner.withBean(S3Clients.class, () -> clientsFor(s3, aws()))
          .withPropertyValues("ecom.asset.client=s3.assets")
          .run(context -> context.getBean(AssetStorage.class).put("images/u-1/a.png", new byte[]{1, 2, 3}, "image/png"));

      ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
      Mockito.verify(s3).putObject(request.capture(), Mockito.any(RequestBody.class));
      assertThat(request.getValue().bucket()).isEqualTo("shop-catalog-sk");
      assertThat(request.getValue().key()).isEqualTo("images/u-1/a.png");
      assertThat(request.getValue().contentType()).isEqualTo("image/png");
      assertThat(request.getValue().cacheControl()).contains("max-age=31536000").contains("immutable");
      assertThat(request.getValue().acl()).as("access is the bucket policy's business").isNull();
   }

   @Test
   void deletesTheFile() {
      S3Client s3 = Mockito.mock(S3Client.class);

      runner.withBean(S3Clients.class, () -> clientsFor(s3, aws()))
          .withPropertyValues("ecom.asset.client=s3.assets")
          .run(context -> context.getBean(AssetStorage.class).delete("images/u-1/a.png"));

      ArgumentCaptor<DeleteObjectRequest> request = ArgumentCaptor.forClass(DeleteObjectRequest.class);
      Mockito.verify(s3).deleteObject(request.capture());
      assertThat(request.getValue().bucket()).isEqualTo("shop-catalog-sk");
      assertThat(request.getValue().key()).isEqualTo("images/u-1/a.png");
   }

   @Test
   void theAddressIsTheBucketsOwnUnlessAnEndpointOrACdnIsConfigured() {
      S3Client s3 = Mockito.mock(S3Client.class);
      var local = new S3ClientProperties.Bucket("ecom-assets", "ap-south-1", "http://localhost:9090/", true, null, "k", "s");
      var cdn = new S3ClientProperties.Bucket("shop-catalog-sk", "ap-south-1", null, null, "https://cdn.example.com/", null, null);

      runner.withBean(S3Clients.class, () -> clientsFor(s3, aws())).withPropertyValues("ecom.asset.client=s3.assets")
          .run(context -> assertThat(context.getBean(AssetStorage.class).publicUrl("images/u-1/a.png"))
              .isEqualTo("https://shop-catalog-sk.s3.ap-south-1.amazonaws.com/images/u-1/a.png"));
      runner.withBean(S3Clients.class, () -> clientsFor(s3, local)).withPropertyValues("ecom.asset.client=s3.assets")
          .run(context -> assertThat(context.getBean(AssetStorage.class).publicUrl("images/u-1/a.png"))
              .isEqualTo("http://localhost:9090/ecom-assets/images/u-1/a.png"));
      runner.withBean(S3Clients.class, () -> clientsFor(s3, cdn)).withPropertyValues("ecom.asset.client=s3.assets")
          .run(context -> assertThat(context.getBean(AssetStorage.class).publicUrl("images/u-1/a.png"))
              .isEqualTo("https://cdn.example.com/images/u-1/a.png"));
   }

   @Test
   void aChoiceWithoutANameFailsStartup() {
      runner.withBean(S3Clients.class, () -> Mockito.mock(S3Clients.class))
          .withPropertyValues("ecom.asset.client=s3.")
          .run(context -> assertThat(context).hasFailed());
   }

   @Test
   void storageDefinedByTheServiceWins() {
      AssetStorage own = Mockito.mock(AssetStorage.class);
      runner.withBean(AssetStorage.class, () -> own)
          .withBean(S3Clients.class, () -> Mockito.mock(S3Clients.class))
          .withPropertyValues("ecom.asset.client=s3.assets")
          .run(context -> assertThat(context.getBean(AssetStorage.class)).isSameAs(own));
   }
}
