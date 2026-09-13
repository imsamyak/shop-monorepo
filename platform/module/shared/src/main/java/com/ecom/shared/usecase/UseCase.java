package com.ecom.shared.usecase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Root contract for all inbound use cases.
 *
 * @param <I> input type (command, query or event)
 * @param <O> output type
 */
@FunctionalInterface
public interface UseCase<I, O> {

   O execute(@NotNull @Valid I input);
}
