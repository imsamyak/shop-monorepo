package com.ecom.listing.service;

import com.ecom.listing.service.product.ImageUrlPolicy;
import com.ecom.shared.exception.validation.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageUrlPolicyTest {

   private static final String OURS = "https://shop-catalog-sk.s3.ap-south-1.amazonaws.com/images/";

   private final ImageUrlPolicy open = new ImageUrlPolicy(List.of());
   private final ImageUrlPolicy ownBucketOnly = new ImageUrlPolicy(List.of(OURS));

   @Test
   void noImagesIsFine() {
      assertThatCode(() -> open.check(null)).doesNotThrowAnyException();
      assertThatCode(() -> open.check(List.of())).doesNotThrowAnyException();
   }

   @Test
   void anyWebAddressIsFineWhenNoPrefixIsConfigured() {
      assertThatCode(() -> open.check(List.of("https://a.example.com/1.png", "http://b.example.com/2.jpg")))
         .doesNotThrowAnyException();
   }

   @Test
   void onlyOurOwnFilesAreAcceptedWhenAPrefixIsConfigured() {
      assertThatCode(() -> ownBucketOnly.check(List.of(OURS + "u-1/a.png"))).doesNotThrowAnyException();
      assertThatThrownBy(() -> ownBucketOnly.check(List.of("https://evil.example.com/tracking.png")))
         .isInstanceOf(ValidationException.class).hasMessageContaining("not one of our uploaded files");
      assertThatThrownBy(() -> ownBucketOnly.check(List.of(OURS + "u-1/a.png", "https://evil.example.com/b.png")))
         .isInstanceOf(ValidationException.class);
   }

   @Test
   void severalPrefixesAreAllowed() {
      ImageUrlPolicy twoBuckets = new ImageUrlPolicy(List.of(OURS, " https://cdn.example.com/images/ "));

      assertThatCode(() -> twoBuckets.check(List.of(OURS + "a.png", "https://cdn.example.com/images/b.png")))
         .doesNotThrowAnyException();
   }

   @Test
   void badShapesAreRejected() {
      assertThatThrownBy(() -> open.check(List.of("javascript:alert(1)"))).isInstanceOf(ValidationException.class);
      assertThatThrownBy(() -> open.check(List.of("//no-scheme.example.com/a.png"))).isInstanceOf(ValidationException.class);
      assertThatThrownBy(() -> open.check(List.of("https:///no-host.png"))).isInstanceOf(ValidationException.class);
      assertThatThrownBy(() -> open.check(java.util.Arrays.asList("https://a.example.com/1.png", null)))
         .isInstanceOf(ValidationException.class);
      assertThatThrownBy(() -> open.check(List.of("https://a.example.com/1.png", "https://a.example.com/1.png")))
         .isInstanceOf(ValidationException.class).hasMessageContaining("twice");
   }
}
