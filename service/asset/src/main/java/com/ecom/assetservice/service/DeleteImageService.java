package com.ecom.assetservice.service;

import com.ecom.asset.ImageUploader;
import com.ecom.assetservice.port.in.usecase.DeleteImageUseCase;
import com.ecom.assetservice.port.in.usecase.dto.command.DeleteImageCmd;
import com.ecom.security.Caller;
import com.ecom.security.UserRole;
import com.ecom.shared.dto.result.Result;
import com.ecom.shared.exception.security.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * Removes an image. Only the user who uploaded it (the owner is part of the key) or an admin may.
 */
@Service
@Validated
@RequiredArgsConstructor
public class DeleteImageService implements DeleteImageUseCase {

   private final ImageUploader imageUploader;

   @Override
   public Result.None execute(DeleteImageCmd cmd) {
      boolean owner = ImageUploader.isOwnedBy(Caller.principal().userId(), cmd.key());
      if (!owner && !Caller.hasRole(UserRole.ROLE_ADMIN)) {
         throw ForbiddenException.of("You do not own this image");
      }
      imageUploader.delete(cmd.key());
      return new Result.None() {
      };
   }
}
