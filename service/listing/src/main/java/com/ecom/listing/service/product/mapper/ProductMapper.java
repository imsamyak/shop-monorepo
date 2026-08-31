package com.ecom.listing.service.product.mapper;

import com.ecom.listing.domain.entity.Product;
import com.ecom.listing.port.in.usecase.product.dto.command.UpdateProductCmd;
import com.ecom.listing.port.in.usecase.product.dto.result.ProductResult;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.Collection;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

   @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
   @Mapping(target = "description", ignore = true)
   void updateEntity(@MappingTarget Product product, UpdateProductCmd cmd);

   @AfterMapping
   default void applyDescription(@MappingTarget Product product, UpdateProductCmd cmd) {
      if (cmd.description() != null) {
         cmd.description().ifSpecified(product::setDescription);
      }
   }

   @Mapping(target = "version", expression = "java(product.version())")
   @Mapping(target = "deletedSkuIds", ignore = true)
   ProductResult toResult(Product product);

   List<ProductResult> toResultList(Collection<Product> products);
}
