package com.ecom.listing.service.product;

import com.ecom.shared.exception.validation.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * What a product's picture list may contain: at most {@value #MAX_IMAGES} addresses, each a distinct http(s) address
 * of at most {@value #MAX_LENGTH} characters. When {@code listing.images.allowed-prefixes} is set, every address must
 * start with one of the prefixes, so only files from our own bucket can be attached (the asset service returns
 * addresses that do). The order of the list is kept as it is.
 */
@Component
public class ImageUrlPolicy {

   static final int MAX_IMAGES = 10;
   static final int MAX_LENGTH = 500;

   private final List<String> allowedPrefixes;

   public ImageUrlPolicy(@Value("${listing.images.allowed-prefixes:}") List<String> allowedPrefixes) {
      this.allowedPrefixes = allowedPrefixes.stream().map(String::trim).filter(prefix -> !prefix.isEmpty()).toList();
   }

   /**
    * @param images the list to check; {@code null} (no pictures) is fine
    * @throws ValidationException if the list breaks a rule
    */
   public void check(List<String> images) {
      if (images == null) {
         return;
      }
      if (images.size() > MAX_IMAGES) {
         throw new ValidationException("A product can have at most " + MAX_IMAGES + " images");
      }
      Set<String> seen = new HashSet<>();
      for (String url : images) {
         if (url == null || url.isBlank() || url.length() > MAX_LENGTH || !isWebAddress(url)) {
            throw new ValidationException("Every image must be an http(s) address of at most " + MAX_LENGTH + " characters");
         }
         if (!allowedPrefixes.isEmpty() && allowedPrefixes.stream().noneMatch(url::startsWith)) {
            throw new ValidationException("Image " + url + " is not one of our uploaded files");
         }
         if (!seen.add(url)) {
            throw new ValidationException("Image " + url + " is listed twice");
         }
      }
   }

   private static boolean isWebAddress(String url) {
      try {
         URI uri = URI.create(url);
         return ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) && uri.getHost() != null;
      } catch (IllegalArgumentException e) {
         return false;
      }
   }
}
