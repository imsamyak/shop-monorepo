package com.ecom.checkout.service.mapper;

import com.ecom.checkout.domain.entity.Cart;
import com.ecom.checkout.port.in.usecase.dto.result.CartItemResult;
import com.ecom.checkout.port.in.usecase.dto.result.CartResult;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
public class CartMapper {

   public CartResult toResult(Cart cart) {
      List<CartItemResult> items = cart.getItems().stream()
            .sorted(Comparator.comparing(i -> i.getSkuId()))
            .map(i -> new CartItemResult(i.getSkuId(), i.getQuantity()))
            .toList();
      return new CartResult(cart.getCustomerId(), items, cart.version(), cart.getUpdatedAt());
   }

   /** What a customer without a stored cart sees. */
   public CartResult toEmptyResult(UUID customerId) {
      return new CartResult(customerId, List.of(), 0, null);
   }
}
