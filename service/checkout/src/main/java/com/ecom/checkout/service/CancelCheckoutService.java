package com.ecom.checkout.service;

import com.ecom.checkout.domain.entity.Checkout;
import com.ecom.checkout.domain.entity.CheckoutItem;
import com.ecom.checkout.domain.entity.CheckoutStatus;
import com.ecom.checkout.domain.exception.CheckoutNotFoundException;
import com.ecom.checkout.domain.exception.InvalidCheckoutStateException;
import com.ecom.checkout.port.in.usecase.CancelCheckoutUseCase;
import com.ecom.checkout.port.in.usecase.dto.command.CancelCheckoutCmd;
import com.ecom.checkout.port.in.usecase.dto.result.CheckoutResult;
import com.ecom.checkout.port.in.usecase.record.RecordCheckouts;
import com.ecom.checkout.port.out.client.inventory.ReleaseStockPort;
import com.ecom.checkout.port.out.persistence.LoadCheckoutPort;
import com.ecom.checkout.service.mapper.CheckoutMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

/**
 * Cancels a draft checkout and returns its stock. The release is atomic and idempotent and a failure is reported (not
 * swallowed), so a cancelled checkout never silently keeps stock reserved: the caller retries until it succeeds.
 */
@Service
@Validated
@RequiredArgsConstructor
public class CancelCheckoutService implements CancelCheckoutUseCase {

   private final LoadCheckoutPort loadCheckoutPort;
   private final ReleaseStockPort releaseStockPort;
   private final RecordCheckouts.RecordCancel recordCancel;
   private final CheckoutMapper checkoutMapper;

   @Override
   public CheckoutResult execute(CancelCheckoutCmd cmd) {
      Checkout checkout = loadCheckoutPort.loadById(cmd.checkoutId())
            .orElseThrow(() -> CheckoutNotFoundException.of(cmd.checkoutId()));

      if (checkout.getStatus() == CheckoutStatus.CANCELLED || checkout.getStatus() == CheckoutStatus.EXPIRED) {
         return checkoutMapper.toResult(checkout);
      }
      if (checkout.getStatus() != CheckoutStatus.DRAFT) {
         throw InvalidCheckoutStateException.of(checkout.getId(), checkout.getStatus(), "cancel");
      }

      List<UUID> reservationIds = checkout.getItems().stream()
            .map(CheckoutItem::getReservationId)
            .toList();
      releaseStockPort.release(reservationIds);

      return recordCancel.execute(new RecordCheckouts.CancelCmd(cmd.checkoutId())).checkout();
   }
}
