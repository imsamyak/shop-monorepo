package com.ecom.checkout.service;

import com.ecom.checkout.domain.entity.Checkout;
import com.ecom.checkout.domain.exception.CheckoutNotFoundException;
import com.ecom.checkout.port.in.usecase.GetCheckoutUseCase;
import com.ecom.checkout.port.in.usecase.dto.command.GetCheckoutQuery;
import com.ecom.checkout.port.in.usecase.dto.result.CheckoutResult;
import com.ecom.checkout.port.out.persistence.LoadCheckoutPort;
import com.ecom.checkout.service.mapper.CheckoutMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class GetCheckoutService implements GetCheckoutUseCase {

   private final LoadCheckoutPort loadCheckoutPort;
   private final CheckoutMapper checkoutMapper;

   @Override
   public CheckoutResult execute(GetCheckoutQuery query) {
      Checkout checkout = loadCheckoutPort.loadById(query.checkoutId())
            .orElseThrow(() -> CheckoutNotFoundException.of(query.checkoutId()));
      return checkoutMapper.toResult(checkout);
   }
}
