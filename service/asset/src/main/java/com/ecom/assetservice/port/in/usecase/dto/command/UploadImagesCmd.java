package com.ecom.assetservice.port.in.usecase.dto.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * @param ownerId the signed-in user the images belong to
 * @param images  the files' content, in the order the caller wants them back
 */
public record UploadImagesCmd(
      @NotBlank String ownerId,
      @NotEmpty @Size(max = UploadImagesCmd.MAX_IMAGES, message = "At most 10 images per upload") List<byte[]> images
) {
   public static final int MAX_IMAGES = 10;
}
