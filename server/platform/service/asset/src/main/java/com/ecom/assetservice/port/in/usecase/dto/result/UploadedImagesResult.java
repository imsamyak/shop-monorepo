package com.ecom.assetservice.port.in.usecase.dto.result;

import com.ecom.asset.StoredImage;
import com.ecom.shared.dto.result.Result;

import java.util.List;

/**
 * @param items the stored images, in the order the files were sent
 */
public record UploadedImagesResult(List<StoredImage> items) implements Result.Many<StoredImage> {

   public UploadedImagesResult {
      items = items == null ? List.of() : List.copyOf(items);
   }
}
