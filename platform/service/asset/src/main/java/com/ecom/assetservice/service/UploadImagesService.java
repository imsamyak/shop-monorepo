package com.ecom.assetservice.service;

import com.ecom.asset.ImageUploader;
import com.ecom.assetservice.port.in.usecase.UploadImagesUseCase;
import com.ecom.assetservice.port.in.usecase.dto.command.UploadImagesCmd;
import com.ecom.assetservice.port.in.usecase.dto.result.UploadedImagesResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class UploadImagesService implements UploadImagesUseCase {

   private final ImageUploader imageUploader;

   @Override
   public UploadedImagesResult execute(UploadImagesCmd cmd) {
      return new UploadedImagesResult(imageUploader.upload(cmd.ownerId(), cmd.images()));
   }
}
