package com.ecom.shared.dto.result;

/**
 * Boundary for command use-case results: {@link Result.One}, {@link Result.Many} or {@link Result.None}.
 */
public sealed interface CommandResult extends Result permits Result.One, Result.Many, Result.None {

}
