package com.ecom.assetservice.adapter.in.web.controller.dto.response;

import com.ecom.asset.StoredImage;
import com.ecom.assetservice.port.in.usecase.dto.result.UploadedImagesResult;

import java.util.List;

/**
 * @param items the uploaded images, in the order the files were sent: the first is the one to show first
 */
public record UploadedImagesResponse(List<Image> items) {

   public static UploadedImagesResponse from(UploadedImagesResult result) {
      return new UploadedImagesResponse(result.items().stream().map(Image::from).toList());
   }

   public record Image(String key, String url, String contentType, long size) {

      static Image from(StoredImage image) {
         return new Image(image.key(), image.url(), image.contentType(), image.size());
      }
   }
}
