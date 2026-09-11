package com.ecom.assetservice.port.in.usecase.dto.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param key the key of an image, as returned when it was uploaded
 */
public record DeleteImageCmd(@NotBlank @Size(max = 300) String key) {
}
