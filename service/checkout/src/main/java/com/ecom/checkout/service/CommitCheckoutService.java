package com.ecom.checkout.service;

import com.ecom.checkout.domain.entity.Checkout;
import com.ecom.checkout.domain.entity.CheckoutItem;
import com.ecom.checkout.domain.entity.CheckoutStatus;
import com.ecom.checkout.domain.exception.CheckoutExpiredException;
import com.ecom.checkout.domain.exception.CheckoutNotFoundException;
import com.ecom.checkout.domain.exception.InvalidCheckoutStateException;
import com.ecom.checkout.port.in.usecase.CommitCheckoutUseCase;
import com.ecom.checkout.port.in.usecase.dto.command.CommitCheckoutCmd;
import com.ecom.checkout.port.in.usecase.dto.result.CheckoutResult;
import com.ecom.checkout.port.in.usecase.record.RecordCheckouts;
import com.ecom.checkout.port.out.client.inventory.CommitStockPort;
import com.ecom.checkout.port.out.persistence.LoadCheckoutPort;
import com.ecom.checkout.service.mapper.CheckoutMapper;
import com.ecom.security.OwnershipGuard;
import com.ecom.security.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Commits a checkout. Inventory is asked first, in one atomic and idempotent call; only then is the checkout recorded
 * as committed. If recording fails the caller simply retries: committing the stock again is a no-op. Committing an
 * already committed checkout returns it unchanged.
 */
@Service
@Validated
@RequiredArgsConstructor
public class CommitCheckoutService implements CommitCheckoutUseCase {

   private final LoadCheckoutPort loadCheckoutPort;
   private final CommitStockPort commitStockPort;
   private final RecordCheckouts.RecordCommit recordCommit;
   private final CheckoutMapper checkoutMapper;

   @Override
   public CheckoutResult execute(CommitCheckoutCmd cmd) {
      Checkout checkout = loadCheckoutPort.loadById(cmd.checkoutId())
            .orElseThrow(() -> CheckoutNotFoundException.of(cmd.checkoutId()));
      OwnershipGuard.requireUserOr(checkout.getCustomerId(), UserRole.ROLE_ADMIN);

      if (checkout.getStatus() == CheckoutStatus.COMMITTED) {
         return checkoutMapper.toResult(checkout);
      }
      if (checkout.getStatus() != CheckoutStatus.DRAFT) {
         throw InvalidCheckoutStateException.of(checkout.getId(), checkout.getStatus(), "commit");
      }
      if (checkout.isExpired(Instant.now())) {
         throw CheckoutExpiredException.of(checkout.getId(), checkout.getExpiresAt());
      }

      List<UUID> reservationIds = checkout.getItems().stream()
            .map(CheckoutItem::getReservationId)
            .toList();
      commitStockPort.commit(reservationIds);

      return recordCommit.execute(new RecordCheckouts.CommitCmd(cmd.checkoutId())).checkout();
   }
}
