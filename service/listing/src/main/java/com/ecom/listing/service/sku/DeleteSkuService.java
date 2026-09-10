package com.ecom.listing.service.sku;

import com.ecom.contract.product.Sku.Deleted;
import com.ecom.listing.domain.entity.Sku;
import com.ecom.listing.domain.exception.sku.SkuNotFoundException;
import com.ecom.listing.port.in.usecase.sku.DeleteSkuUseCase;
import com.ecom.listing.port.in.usecase.sku.dto.command.DeleteSkuCmd;
import com.ecom.listing.port.in.usecase.sku.dto.result.SkuResult;
import com.ecom.listing.port.out.persistence.sku.DeleteSkuPort;
import com.ecom.listing.service.sku.mapper.SkuMapper;
import com.ecom.security.OwnershipGuard;
import com.ecom.security.UserRole;
import com.ecom.shared.messaging.DomainEvent;
import com.ecom.shared.validator.EntityVersionValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class DeleteSkuService implements DeleteSkuUseCase {

   private final DeleteSkuPort deleteSkuPort;
   private final SkuMapper skuMapper;
   private final EntityVersionValidator versionValidator;

   @Override
   @Transactional
   public SkuResult execute(DeleteSkuCmd cmd) {
      Sku sku = deleteSkuPort.loadForUpdate(cmd.productId(), cmd.skuId())
          .orElseThrow(() -> SkuNotFoundException.of(cmd.productId(), cmd.skuId()));

      OwnershipGuard.requireSellerOr(sku.getSellerId(), UserRole.ROLE_ADMIN);
      versionValidator.validate(sku, cmd.version());

      sku.softDelete();
      return skuMapper.toResult(deleteSkuPort.save(sku));
   }

   @Override
   public DomainEvent buildEvent(DeleteSkuCmd cmd, SkuResult data) {
      if (data == null) {
         return null;
      }
      return new Deleted(
          data.skuId().toString(),
          data.productId().toString(),
          data.version()
      );
   }
}
