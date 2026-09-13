package com.ecom.inventory.port.in.usecase.reservation.dto.command;

import lombok.Builder;

import java.time.Instant;

/**
 * @param referenceTime treat reservations expiring before this instant as expired (defaults to now)
 * @param limit         maximum number of reservations to expire in one call (defaults to 500)
 */
@Builder
public record ExpireReservationsCmd(
      Instant referenceTime,
      Integer limit
) {
   public static final int DEFAULT_LIMIT = 500;

   public ExpireReservationsCmd() {
      this(null, null);
   }

   public int effectiveLimit() {
      return limit == null || limit <= 0 ? DEFAULT_LIMIT : limit;
   }
}
