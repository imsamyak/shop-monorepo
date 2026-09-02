package com.ecom.checkout.port.in.usecase.record;

import com.ecom.checkout.port.in.usecase.dto.result.RecordedCheckout;
import com.ecom.outbox.OutboxUseCase;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The short local transactions behind the checkout use cases. Each one changes the checkout and writes its outbox
 * event atomically; the orchestrating use cases call them AFTER (never during) their calls to other services, so a
 * database transaction is never held open across the network.
 */
public final class RecordCheckouts {

   private RecordCheckouts() {
   }

   public record DraftCmd(UUID checkoutId, UUID customerId, Instant expiresAt, List<Item> items) {
      public record Item(Long skuId, UUID sellerId, int quantity, BigDecimal unitPrice, UUID reservationId) {
      }
   }

   public record CommitCmd(UUID checkoutId) {
   }

   public record CancelCmd(UUID checkoutId) {
   }

   public record ExpireCmd(UUID checkoutId) {
   }

   public interface RecordDraft extends OutboxUseCase<DraftCmd, RecordedCheckout> {
   }

   public interface RecordCommit extends OutboxUseCase<CommitCmd, RecordedCheckout> {
   }

   public interface RecordCancel extends OutboxUseCase<CancelCmd, RecordedCheckout> {
   }

   public interface RecordExpiry extends OutboxUseCase<ExpireCmd, RecordedCheckout> {
   }
}
