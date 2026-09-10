package com.ecom.checkout.service;

import com.ecom.checkout.port.in.usecase.GetCartUseCase;
import com.ecom.checkout.port.in.usecase.dto.command.GetCartQuery;
import com.ecom.checkout.port.in.usecase.dto.result.CartResult;
import com.ecom.checkout.port.out.persistence.LoadCartPort;
import com.ecom.checkout.service.mapper.CartMapper;
import com.ecom.security.OwnershipGuard;
import com.ecom.security.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class GetCartService implements GetCartUseCase {

   private final LoadCartPort loadCartPort;
   private final CartMapper cartMapper;

   @Override
   public CartResult execute(GetCartQuery query) {
      OwnershipGuard.requireUserOr(query.customerId(), UserRole.ROLE_ADMIN);
      return loadCartPort.loadByCustomerId(query.customerId())
            .map(cartMapper::toResult)
            .orElseGet(() -> cartMapper.toEmptyResult(query.customerId()));
   }
}
