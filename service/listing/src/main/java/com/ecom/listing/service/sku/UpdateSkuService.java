package com.ecom.listing.service.sku;

import com.ecom.contract.product.Sku.Updated;
import com.ecom.listing.domain.entity.Sku;
import com.ecom.listing.domain.exception.sku.DuplicateSkuPropertiesException;
import com.ecom.listing.domain.exception.sku.SkuNotFoundException;
import com.ecom.listing.port.in.usecase.sku.UpdateSkuUseCase;
import com.ecom.listing.port.in.usecase.sku.dto.command.UpdateSkuCmd;
import com.ecom.listing.port.in.usecase.sku.dto.result.SkuResult;
import com.ecom.listing.port.out.persistence.sku.LoadSkuPort;
import com.ecom.listing.port.out.persistence.sku.UpdateSkuPort;
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
public class UpdateSkuService implements UpdateSkuUseCase {

   private final UpdateSkuPort updateSkuPort;
   private final LoadSkuPort loadSkuPort;
   private final SkuMapper skuMapper;
   private final EntityVersionValidator versionValidator;

   @Override
   @Transactional
   public SkuResult execute(UpdateSkuCmd cmd) {
      Sku sku = updateSkuPort.loadForUpdate(cmd.productId(), cmd.skuId())
          .orElseThrow(() -> SkuNotFoundException.of(cmd.productId(), cmd.skuId()));

      OwnershipGuard.requireSellerOr(sku.getSellerId(), UserRole.ROLE_ADMIN);
      versionValidator.validate(sku, cmd.version());

      skuMapper.updateEntity(sku, cmd);

      if (cmd.properties() != null
          && loadSkuPort.existsWithProperties(cmd.productId(), sku.getProperties(), sku.getId())) {
         throw DuplicateSkuPropertiesException.of(cmd.productId(), sku.getProperties());
      }

      return skuMapper.toResult(updateSkuPort.save(sku));
   }

   @Override
   public DomainEvent buildEvent(UpdateSkuCmd cmd, SkuResult data) {
      if (data == null) {
         return null;
      }
      return new Updated(
          data.skuId().toString(),
          data.productId().toString(),
          data.price(),
          data.properties(),
          data.version()
      );
   }
}
