package com.ecom.inventory.service.reservation;

import com.ecom.inventory.port.in.usecase.reservation.ExpireReservationsUseCase;
import com.ecom.inventory.port.in.usecase.reservation.dto.command.ExpireReservationsCmd;
import com.ecom.inventory.port.in.usecase.reservation.dto.result.ExpireReservationsResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically returns the stock of reservations nobody committed. Without this, abandoned checkouts would hold stock
 * forever. Disable with {@code inventory.reservation.expiry-enabled=false}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "inventory.reservation", name = "expiry-enabled", havingValue = "true", matchIfMissing = true)
public class ReservationExpiryScheduler {

   private final ExpireReservationsUseCase expireReservationsUseCase;

   @Scheduled(fixedDelayString = "${inventory.reservation.expiry-interval-ms:30000}")
   public void expireOverdueReservations() {
      try {
         int limit = ExpireReservationsCmd.DEFAULT_LIMIT;
         ExpireReservationsResult result;
         do {
            result = expireReservationsUseCase.execute(new ExpireReservationsCmd(null, limit));
            if (result.expiredCount() > 0) {
               log.info("Expired {} overdue reservation(s)", result.expiredCount());
            }
         } while (result.expiredCount() >= limit);
      } catch (Exception e) {
         log.error("Reservation expiry run failed; will retry on the next tick", e);
      }
   }
}
