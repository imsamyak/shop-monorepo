package com.ecom.assetservice.port.in.usecase;

import com.ecom.assetservice.port.in.usecase.dto.command.UploadImagesCmd;
import com.ecom.assetservice.port.in.usecase.dto.result.UploadedImagesResult;
import com.ecom.shared.usecase.CommandUseCase;

public interface UploadImagesUseCase extends CommandUseCase<UploadImagesCmd, UploadedImagesResult> {
}
