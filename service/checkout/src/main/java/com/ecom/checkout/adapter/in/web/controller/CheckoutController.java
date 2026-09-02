package com.ecom.checkout.adapter.in.web.controller;

import com.ecom.checkout.adapter.in.web.controller.dto.request.DraftCheckoutRequest;
import com.ecom.checkout.adapter.in.web.controller.dto.response.CheckoutResponse;
import com.ecom.checkout.port.in.usecase.CancelCheckoutUseCase;
import com.ecom.checkout.port.in.usecase.CommitCheckoutUseCase;
import com.ecom.checkout.port.in.usecase.DraftCheckoutUseCase;
import com.ecom.checkout.port.in.usecase.GetCheckoutUseCase;
import com.ecom.checkout.port.in.usecase.dto.command.CancelCheckoutCmd;
import com.ecom.checkout.port.in.usecase.dto.command.CommitCheckoutCmd;
import com.ecom.checkout.port.in.usecase.dto.command.GetCheckoutQuery;
import com.ecom.checkout.port.in.usecase.dto.result.CheckoutResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * Nothing here checks who is calling.
 */
@RestController
@RequestMapping("/checkouts")
@RequiredArgsConstructor
public class CheckoutController {

   private final DraftCheckoutUseCase draftCheckoutUseCase;
   private final CommitCheckoutUseCase commitCheckoutUseCase;
   private final CancelCheckoutUseCase cancelCheckoutUseCase;
   private final GetCheckoutUseCase getCheckoutUseCase;

   @PostMapping
   public ResponseEntity<CheckoutResponse> draft(@Valid @RequestBody DraftCheckoutRequest request) {
      CheckoutResult result = draftCheckoutUseCase.execute(request.toCommand());
      CheckoutResponse response = CheckoutResponse.from(result);
      return ResponseEntity.created(
          URI.create("/checkouts/" + response.checkoutId())).body(response);
   }

   @GetMapping("/{checkoutId}")
   public ResponseEntity<CheckoutResponse> get(@PathVariable UUID checkoutId) {
      return ResponseEntity.ok(CheckoutResponse.from(getCheckoutUseCase.execute(new GetCheckoutQuery(checkoutId))));
   }

   @PostMapping("/{checkoutId}/commit")
   public ResponseEntity<CheckoutResponse> commit(@PathVariable UUID checkoutId) {
      return ResponseEntity.ok(CheckoutResponse.from(commitCheckoutUseCase.execute(new CommitCheckoutCmd(checkoutId))));
   }

   @PostMapping("/{checkoutId}/cancel")
   public ResponseEntity<CheckoutResponse> cancel(@PathVariable UUID checkoutId) {
      return ResponseEntity.ok(CheckoutResponse.from(cancelCheckoutUseCase.execute(new CancelCheckoutCmd(checkoutId))));
   }
}
