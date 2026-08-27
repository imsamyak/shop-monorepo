package com.ecom.shared.dto.result;

/**
 * Boundary for query use-case results: {@link Result.One}, {@link Result.Many} or {@link Result.Page}.
 */
public sealed interface QueryResult extends Result permits Result.One, Result.Many, Result.Page {

}
