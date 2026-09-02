package com.ecom.checkout.port.in.usecase.dto.result;

import com.ecom.shared.dto.result.Result;

/**
 * Outcome of one of the transactional recording steps.
 *
 * @param changed whether this call changed state. An idempotent replay returns {@code false} and emits no event.
 */
public record RecordedCheckout(CheckoutResult checkout, boolean changed) implements Result.One {
}
