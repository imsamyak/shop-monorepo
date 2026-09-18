package com.ecom.listing.adapter.in.web.controller.sku;

import com.ecom.listing.adapter.in.web.controller.sku.dto.response.SkuResponse;
import com.ecom.listing.port.in.usecase.sku.LookupSkusUseCase;
import com.ecom.listing.port.in.usecase.sku.dto.query.LookupSkusQuery;
import com.ecom.security.RequireRole;
import com.ecom.security.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Looks SKUs up by id alone, for callers that do not know the product (for example {@code GET /skus?skuIds=3,4}).
 * Internal: only other services (checkout prices a cart with it) and admins. The query string is bound straight into
 * {@link LookupSkusQuery}.
 */
@RestController
@RequestMapping("/skus")
@RequireRole({UserRole.ROLE_SYSTEM, UserRole.ROLE_ADMIN})
@RequiredArgsConstructor
public class SkuLookupController {

   private final LookupSkusUseCase lookupSkusUseCase;

   @GetMapping
   public ResponseEntity<List<SkuResponse>> lookup(@Valid LookupSkusQuery query) {
      return ResponseEntity.ok(lookupSkusUseCase.execute(query).items().stream().map(SkuResponse::from).toList());
   }
}
