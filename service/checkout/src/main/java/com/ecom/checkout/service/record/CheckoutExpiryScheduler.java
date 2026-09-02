package com.ecom.checkout.service.record;

import com.ecom.checkout.port.in.usecase.record.RecordCheckouts;
import com.ecom.checkout.port.out.persistence.LoadCheckoutPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Marks drafts nobody committed as expired. Inventory returns the held stock by itself when the reservation expires,
 * so nothing is called here; each checkout is expired in its own transaction and emits {@code Checkout.Cancelled}.
 * Disable with {@code checkout.expiry.enabled=false}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "checkout.expiry", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CheckoutExpiryScheduler {

   private static final int BATCH = 100;

   private final LoadCheckoutPort loadCheckoutPort;
   private final RecordCheckouts.RecordExpiry recordExpiry;

   @Scheduled(fixedDelayString = "${checkout.expiry.interval-ms:60000}")
   public void expireOverdueDrafts() {
      List<UUID> overdue;
      int expired;
      do {
         overdue = loadCheckoutPort.findExpiredDraftIds(Instant.now(), BATCH);
         expired = 0;
         for (UUID id : overdue) {
            try {
               recordExpiry.execute(new RecordCheckouts.ExpireCmd(id));
               expired++;
            } catch (Exception e) {
               log.error("Could not expire checkout {}; will retry on the next tick", id, e);
            }
         }
         // Keep going only while progress is made; a checkout that keeps failing must not spin this loop.
      } while (overdue.size() >= BATCH && expired > 0);
   }
}
