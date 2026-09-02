package com.ecom.checkout.service.record;

import com.ecom.checkout.domain.entity.Checkout;
import com.ecom.checkout.domain.entity.CheckoutItem;
import com.ecom.checkout.domain.entity.CheckoutStatus;
import com.ecom.checkout.domain.exception.CheckoutNotFoundException;
import com.ecom.checkout.domain.exception.InvalidCheckoutStateException;
import com.ecom.checkout.port.in.usecase.dto.result.CheckoutResult;
import com.ecom.checkout.port.in.usecase.dto.result.RecordedCheckout;
import com.ecom.checkout.port.in.usecase.record.RecordCheckouts;
import com.ecom.checkout.port.out.persistence.LoadCheckoutForUpdatePort;
import com.ecom.checkout.port.out.persistence.SaveCheckoutPort;
import com.ecom.checkout.service.mapper.CheckoutMapper;
import com.ecom.contract.checkout.Checkout.Cancelled;
import com.ecom.contract.checkout.Checkout.Committed;
import com.ecom.contract.checkout.Checkout.Drafted;
import com.ecom.contract.checkout.Checkout.Item;
import com.ecom.shared.messaging.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * The transactional recording steps of the checkout use cases. Each is one short local transaction (the outbox aspect
 * adds the event row to it) and none of them calls another service.
 */
public final class RecordCheckoutServices {

   private RecordCheckoutServices() {
   }

   private static List<Item> eventItems(CheckoutResult checkout) {
      return checkout.items().stream()
            .map(i -> new Item(String.valueOf(i.skuId()), i.quantity(), i.unitPrice(), i.sellerId().toString()))
            .toList();
   }

   @Service
   @RequiredArgsConstructor
   public static class RecordDraftService implements RecordCheckouts.RecordDraft {

      private final SaveCheckoutPort saveCheckoutPort;
      private final CheckoutMapper checkoutMapper;

      @Override
      @Transactional
      public RecordedCheckout execute(RecordCheckouts.DraftCmd cmd) {
         List<CheckoutItem> items = cmd.items().stream()
               .map(i -> CheckoutItem.of(i.skuId(), i.sellerId(), i.quantity(), i.unitPrice(), i.reservationId()))
               .toList();
         Checkout checkout = Checkout.draft(cmd.checkoutId(), cmd.customerId(), cmd.expiresAt(), items);
         return new RecordedCheckout(checkoutMapper.toResult(saveCheckoutPort.save(checkout)), true);
      }

      @Override
      public DomainEvent buildEvent(RecordCheckouts.DraftCmd cmd, RecordedCheckout result) {
         CheckoutResult checkout = result.checkout();
         return new Drafted(checkout.checkoutId().toString(), checkout.customerId().toString(), eventItems(checkout));
      }
   }

   @Service
   @RequiredArgsConstructor
   public static class RecordCommitService implements RecordCheckouts.RecordCommit {

      private final LoadCheckoutForUpdatePort loadCheckoutForUpdatePort;
      private final SaveCheckoutPort saveCheckoutPort;
      private final CheckoutMapper checkoutMapper;

      @Override
      @Transactional
      public RecordedCheckout execute(RecordCheckouts.CommitCmd cmd) {
         Checkout checkout = loadCheckoutForUpdatePort.loadByIdForUpdate(cmd.checkoutId())
               .orElseThrow(() -> CheckoutNotFoundException.of(cmd.checkoutId()));

         if (checkout.getStatus() == CheckoutStatus.COMMITTED) {
            return new RecordedCheckout(checkoutMapper.toResult(checkout), false);
         }
         checkout.commit(Instant.now());
         return new RecordedCheckout(checkoutMapper.toResult(saveCheckoutPort.save(checkout)), true);
      }

      @Override
      public DomainEvent buildEvent(RecordCheckouts.CommitCmd cmd, RecordedCheckout result) {
         if (!result.changed()) {
            return null;
         }
         CheckoutResult checkout = result.checkout();
         return new Committed(checkout.checkoutId().toString(), checkout.customerId().toString(), eventItems(checkout));
      }
   }

   @Service
   @RequiredArgsConstructor
   public static class RecordCancelService implements RecordCheckouts.RecordCancel {

      private final LoadCheckoutForUpdatePort loadCheckoutForUpdatePort;
      private final SaveCheckoutPort saveCheckoutPort;
      private final CheckoutMapper checkoutMapper;

      @Override
      @Transactional
      public RecordedCheckout execute(RecordCheckouts.CancelCmd cmd) {
         Checkout checkout = loadCheckoutForUpdatePort.loadByIdForUpdate(cmd.checkoutId())
               .orElseThrow(() -> CheckoutNotFoundException.of(cmd.checkoutId()));

         if (checkout.getStatus() == CheckoutStatus.CANCELLED || checkout.getStatus() == CheckoutStatus.EXPIRED) {
            return new RecordedCheckout(checkoutMapper.toResult(checkout), false);
         }
         if (checkout.getStatus() != CheckoutStatus.DRAFT) {
            throw InvalidCheckoutStateException.of(checkout.getId(), checkout.getStatus(), "cancel");
         }
         checkout.cancel();
         return new RecordedCheckout(checkoutMapper.toResult(saveCheckoutPort.save(checkout)), true);
      }

      @Override
      public DomainEvent buildEvent(RecordCheckouts.CancelCmd cmd, RecordedCheckout result) {
         return result.changed() ? new Cancelled(result.checkout().checkoutId().toString(), "USER_CANCELLED") : null;
      }
   }

   @Service
   @RequiredArgsConstructor
   public static class RecordExpiryService implements RecordCheckouts.RecordExpiry {

      private final LoadCheckoutForUpdatePort loadCheckoutForUpdatePort;
      private final SaveCheckoutPort saveCheckoutPort;
      private final CheckoutMapper checkoutMapper;

      @Override
      @Transactional
      public RecordedCheckout execute(RecordCheckouts.ExpireCmd cmd) {
         Checkout checkout = loadCheckoutForUpdatePort.loadByIdForUpdate(cmd.checkoutId())
               .orElseThrow(() -> CheckoutNotFoundException.of(cmd.checkoutId()));

         if (!checkout.expire(Instant.now())) {
            return new RecordedCheckout(checkoutMapper.toResult(checkout), false);
         }
         return new RecordedCheckout(checkoutMapper.toResult(saveCheckoutPort.save(checkout)), true);
      }

      @Override
      public DomainEvent buildEvent(RecordCheckouts.ExpireCmd cmd, RecordedCheckout result) {
         return result.changed() ? new Cancelled(result.checkout().checkoutId().toString(), "EXPIRED") : null;
      }
   }
}
