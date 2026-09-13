package com.ecom.identity.port.in.usecase.dto.result;

import com.ecom.shared.dto.result.Result;

/**
 * The result of a command that has nothing to hand back.
 */
public record Done() implements Result.None {

   public static final Done INSTANCE = new Done();
}
