package com.ecom.assetservice.adapter.in.web.controller;

import com.ecom.assetservice.adapter.in.web.controller.dto.response.UploadedImagesResponse;
import com.ecom.assetservice.port.in.usecase.DeleteImageUseCase;
import com.ecom.assetservice.port.in.usecase.UploadImagesUseCase;
import com.ecom.assetservice.port.in.usecase.dto.command.DeleteImageCmd;
import com.ecom.assetservice.port.in.usecase.dto.command.UploadImagesCmd;
import com.ecom.security.Caller;
import com.ecom.security.RequireRole;
import com.ecom.security.UserRole;
import com.ecom.shared.exception.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Image uploads for sellers and admins. The files go to the bucket and the response gives their public addresses, in
 * the order the files were sent; the caller then attaches those addresses to a product.
 */
@RestController
@RequestMapping("/assets")
@RequireRole({UserRole.ROLE_SELLER, UserRole.ROLE_ADMIN})
@RequiredArgsConstructor
public class AssetController {

   private final UploadImagesUseCase uploadImagesUseCase;
   private final DeleteImageUseCase deleteImageUseCase;

   /**
    * {@code multipart/form-data} with one or more parts named {@code files}. Only JPEG, PNG, GIF and WebP images are
    * accepted; the type is read from the file itself.
    */
   @PostMapping("/images")
   public ResponseEntity<UploadedImagesResponse> upload(@RequestParam("files") List<MultipartFile> files) {
      List<byte[]> contents = new ArrayList<>(files.size());
      for (MultipartFile file : files) {
         try {
            contents.add(file.getBytes());
         } catch (IOException e) {
            throw new UncheckedIOException(e);
         }
      }
      if (contents.isEmpty()) {
         throw new ValidationException("Send at least one file in the 'files' part");
      }
      var result = uploadImagesUseCase.execute(new UploadImagesCmd(Caller.principal().userId(), contents));
      return ResponseEntity.status(201).body(UploadedImagesResponse.from(result));
   }

   /** Removes an image by the {@code key} it was given on upload. */
   @DeleteMapping("/images")
   public ResponseEntity<Void> delete(@RequestParam("key") String key) {
      deleteImageUseCase.execute(new DeleteImageCmd(key));
      return ResponseEntity.noContent().build();
   }
}
