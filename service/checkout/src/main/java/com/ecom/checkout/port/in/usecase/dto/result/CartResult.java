package com.ecom.checkout.port.in.usecase.dto.result;

import com.ecom.shared.dto.result.Result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The cart as stored. A customer who never added anything gets an empty cart at version 0, not a "not found".
 */
public record CartResult(UUID customerId, List<CartItemResult> items, long version, Instant updatedAt) implements Result.One {
}
