package com.ecom.listing.service;

import com.ecom.listing.domain.entity.Product;
import com.ecom.listing.domain.entity.Sku;
import com.ecom.listing.domain.exception.product.ProductNotFoundException;
import com.ecom.listing.domain.exception.sku.SkuNotFoundException;
import com.ecom.listing.port.in.usecase.product.dto.query.GetProductQuery;
import com.ecom.listing.port.in.usecase.product.dto.query.ListProductsQuery;
import com.ecom.listing.port.in.usecase.product.dto.query.ProductSort;
import com.ecom.listing.port.in.usecase.product.dto.result.ProductPageResult;
import com.ecom.listing.port.in.usecase.product.dto.result.ProductResult;
import com.ecom.listing.port.in.usecase.sku.dto.query.GetSkuQuery;
import com.ecom.listing.port.in.usecase.sku.dto.query.ListSkusQuery;
import com.ecom.listing.port.in.usecase.sku.dto.result.SkuListResult;
import com.ecom.listing.port.in.usecase.sku.dto.result.SkuResult;
import com.ecom.listing.port.out.persistence.product.LoadProductPort;
import com.ecom.listing.port.out.persistence.product.SearchProductsPort;
import com.ecom.listing.port.out.persistence.sku.LoadSkuPort;
import com.ecom.listing.service.product.GetProductService;
import com.ecom.listing.service.product.ListProductsService;
import com.ecom.listing.service.product.mapper.ProductMapperImpl;
import com.ecom.listing.service.sku.GetSkuService;
import com.ecom.listing.service.sku.ListSkusService;
import com.ecom.listing.service.sku.mapper.SkuMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class QueryServicesTest {

   private final UUID seller = UUID.randomUUID();
   private final LoadProductPort loadProductPort = mock(LoadProductPort.class);
   private final LoadSkuPort loadSkuPort = mock(LoadSkuPort.class);
   private final SearchProductsPort searchProductsPort = mock(SearchProductsPort.class);

   private Product product(String title) {
      Product product = Product.of(seller, title, "desc", BigDecimal.valueOf(100));
      ReflectionTestUtils.setField(product, "id", UUID.randomUUID());
      return product;
   }

   private Sku sku(Product product, long id) {
      Sku sku = Sku.of(product, BigDecimal.TEN).setProperty("color", "red");
      ReflectionTestUtils.setField(sku, "id", id);
      return sku;
   }

   private ListProductsService listService() {
      return new ListProductsService(searchProductsPort, new ProductMapperImpl());
   }

   // ---------------------------------------------------------------- products

   @Test
   void getProductReturnsTheLiveProduct() {
      Product product = product("Laptop Pro");
      when(loadProductPort.load(product.getId())).thenReturn(Optional.of(product));

      ProductResult result = new GetProductService(loadProductPort, new ProductMapperImpl())
            .execute(new GetProductQuery(product.getId()));

      assertThat(result.id()).isEqualTo(product.getId());
      assertThat(result.title()).isEqualTo("Laptop Pro");
      assertThat(result.sellerId()).isEqualTo(seller);
   }

   @Test
   void getProductFailsWhenTheProductIsMissingOrDeleted() {
      UUID id = UUID.randomUUID();
      when(loadProductPort.load(id)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> new GetProductService(loadProductPort, new ProductMapperImpl())
            .execute(new GetProductQuery(id)))
            .isInstanceOf(ProductNotFoundException.class);
   }

   @Test
   void listProductsReportsThePageAndTheNextPageIndex() {
      when(searchProductsPort.search(seller, "lap", 0, 2, ProductSort.TITLE_ASC))
            .thenReturn(new SearchProductsPort.Found(List.of(product("Laptop A"), product("Laptop B")), 5));

      ProductPageResult result = listService().execute(
            new ListProductsQuery(seller, " lap ", 0, 2, ProductSort.TITLE_ASC));

      assertThat(result.items()).extracting(ProductResult::title).containsExactly("Laptop A", "Laptop B");
      assertThat(result.totalElements()).isEqualTo(5);
      assertThat(result.totalPages()).isEqualTo(3);
      assertThat(result.page()).isZero();
      assertThat(result.cursor()).isEqualTo("1");
   }

   @Test
   void listProductsHasNoCursorOnTheLastPage() {
      when(searchProductsPort.search(null, null, 2, 2, ProductSort.CREATED_AT_DESC))
            .thenReturn(new SearchProductsPort.Found(List.of(product("Laptop E")), 5));

      ProductPageResult result = listService().execute(
            new ListProductsQuery(null, null, 2, 2, ProductSort.CREATED_AT_DESC));

      assertThat(result.cursor()).isNull();
      assertThat(result.items()).hasSize(1);
   }

   @Test
   void listProductsOfAnEmptyResultHasZeroPagesAndTreatsABlankTitleAsNoFilter() {
      when(searchProductsPort.search(null, null, 0, 20, ProductSort.CREATED_AT_DESC))
            .thenReturn(new SearchProductsPort.Found(List.of(), 0));

      ProductPageResult result = listService().execute(
            new ListProductsQuery(null, "   ", 0, 20, ProductSort.CREATED_AT_DESC));

      assertThat(result.items()).isEmpty();
      assertThat(result.totalPages()).isZero();
      assertThat(result.cursor()).isNull();
      verify(searchProductsPort).search(null, null, 0, 20, ProductSort.CREATED_AT_DESC);
   }

   // ---------------------------------------------------------------- skus

   @Test
   void getSkuReturnsTheSkuOfThatProduct() {
      Product product = product("Laptop Pro");
      when(loadSkuPort.load(product.getId(), 7L)).thenReturn(Optional.of(sku(product, 7)));

      SkuResult result = new GetSkuService(loadSkuPort, new SkuMapper()).execute(new GetSkuQuery(product.getId(), 7L));

      assertThat(result.skuId()).isEqualTo(7L);
      assertThat(result.productId()).isEqualTo(product.getId());
      assertThat(result.properties()).containsEntry("color", "red");
   }

   @Test
   void getSkuFailsWhenItDoesNotBelongToTheProduct() {
      UUID productId = UUID.randomUUID();
      when(loadSkuPort.load(productId, 7L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> new GetSkuService(loadSkuPort, new SkuMapper()).execute(new GetSkuQuery(productId, 7L)))
            .isInstanceOf(SkuNotFoundException.class);
   }

   @Test
   void listSkusReturnsEveryLiveSkuOfTheProduct() {
      Product product = product("Laptop Pro");
      when(loadProductPort.load(product.getId())).thenReturn(Optional.of(product));
      when(loadSkuPort.loadByProduct(product.getId())).thenReturn(List.of(sku(product, 1), sku(product, 2)));

      SkuListResult result = new ListSkusService(loadProductPort, loadSkuPort, new SkuMapper())
            .execute(new ListSkusQuery(product.getId()));

      assertThat(result.items()).extracting(SkuResult::skuId).containsExactly(1L, 2L);
   }

   @Test
   void listSkusFailsWhenTheProductIsMissingOrDeleted() {
      UUID productId = UUID.randomUUID();
      when(loadProductPort.load(productId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> new ListSkusService(loadProductPort, loadSkuPort, new SkuMapper())
            .execute(new ListSkusQuery(productId)))
            .isInstanceOf(ProductNotFoundException.class);
      verifyNoInteractions(loadSkuPort);
   }
}
