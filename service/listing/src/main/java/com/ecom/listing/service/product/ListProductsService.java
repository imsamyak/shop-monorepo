package com.ecom.listing.service.product;

import com.ecom.listing.port.in.usecase.product.ListProductsUseCase;
import com.ecom.listing.port.in.usecase.product.dto.query.ListProductsQuery;
import com.ecom.listing.port.in.usecase.product.dto.result.ProductPageResult;
import com.ecom.listing.port.out.persistence.product.SearchProductsPort;
import com.ecom.listing.service.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class ListProductsService implements ListProductsUseCase {

   private final SearchProductsPort searchProductsPort;
   private final ProductMapper productMapper;

   @Override
   public ProductPageResult execute(ListProductsQuery query) {
      String title = query.title() == null || query.title().isBlank() ? null : query.title().strip();
      SearchProductsPort.Found found =
            searchProductsPort.search(query.sellerId(), title, query.page(), query.size(), query.sort());

      int totalPages = (int) ((found.totalElements() + query.size() - 1) / query.size());
      String cursor = query.page() + 1 < totalPages ? String.valueOf(query.page() + 1) : null;

      return new ProductPageResult(productMapper.toResultList(found.items()), cursor, query.page(), query.size(),
            found.totalElements(), totalPages);
   }
}
