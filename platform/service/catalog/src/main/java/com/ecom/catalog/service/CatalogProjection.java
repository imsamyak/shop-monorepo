package com.ecom.catalog.service;

import com.ecom.catalog.domain.entity.CatalogProduct;
import com.ecom.catalog.domain.entity.CatalogSku;
import com.ecom.catalog.port.out.persistence.LoadProductPort;
import com.ecom.catalog.port.out.persistence.LoadSkuPort;
import com.ecom.catalog.port.out.persistence.SaveProductPort;
import com.ecom.catalog.port.out.persistence.SaveSkuPort;
import com.ecom.contract.inventory.Inventory;
import com.ecom.contract.product.Product;
import com.ecom.contract.product.Sku;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.function.UnaryOperator;

/**
 * Applies the events of listing and inventory to the catalog's read model. The ordering rule of the event contract is
 * in the domain records ({@link CatalogProduct}, {@link CatalogSku}): an event that is not newer than what is stored
 * changes nothing, {@code Deleted} is final. Here every event is one load, one change and one atomic save that only
 * succeeds when nobody stored in between (otherwise it starts over), so duplicates and concurrent deliveries are safe.
 * <p>
 * The SKUs and the stock of a SKU are stored per SKU. The product they belong to is then refreshed from all its SKUs
 * with the same save, which repeats until the stored product reflects the SKUs as they were after this event; each SKU
 * change refreshes after its own save, so the product settles on the latest of them whatever the interleaving.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogProjection {

   private static final int MAX_ATTEMPTS = 50;

   private final LoadProductPort loadProductPort;
   private final SaveProductPort saveProductPort;
   private final LoadSkuPort loadSkuPort;
   private final SaveSkuPort saveSkuPort;
   private final Clock clock;

   public void apply(Product.Created event) {
      updateProduct(event.productId(), product -> product.applyCreated(event, clock.instant()));
   }

   public void apply(Product.Updated event) {
      updateProduct(event.productId(), product -> product.applyUpdated(event.title(), event.description(), event.images(), event.version()));
   }

   public void apply(Product.Deleted event) {
      updateProduct(event.productId(), CatalogProduct::applyDeleted);
   }

   public void apply(Sku.Created event) {
      updateSku(event.skuId(), sku -> sku.withSku(event.productId(), event.price(), event.properties(), event.version()));
   }

   public void apply(Sku.Updated event) {
      updateSku(event.skuId(), sku -> sku.withSku(event.productId(), event.price(), event.properties(), event.version()));
   }

   public void apply(Sku.Deleted event) {
      updateSku(event.skuId(), sku -> sku.withDeleted(event.productId()));
   }

   /**
    * The three inventory events are all snapshots of the SKU's stock: the one with the highest version is the current.
    */
   public void apply(Inventory.StockAdded event) {
      applyStock(event.skuId(), event.availableStock(), event.version());
   }

   public void apply(Inventory.StockAdjusted event) {
      applyStock(event.skuId(), event.availableStock(), event.version());
   }

   public void apply(Inventory.AvailabilityChanged event) {
      applyStock(event.skuId(), event.availableStock(), event.version());
   }

   private void applyStock(String skuId, int availableStock, long version) {
      updateSku(skuId, sku -> sku.withStock(availableStock, version));
   }

   private void updateProduct(String productId, UnaryOperator<CatalogProduct> change) {
      for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
         CatalogProduct current = loadProductPort.loadById(productId).orElseGet(() -> CatalogProduct.empty(productId));
         CatalogProduct next = change.apply(current);
         if (next == current || saveProductPort.saveIfUnchanged(next)) {
            return;
         }
      }
      throw new IllegalStateException("Product " + productId + " keeps changing; giving up after " + MAX_ATTEMPTS + " attempts");
   }

   private void updateSku(String skuId, UnaryOperator<CatalogSku> change) {
      for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
         CatalogSku current = loadSkuPort.loadBySkuId(skuId).orElseGet(() -> CatalogSku.empty(skuId));
         CatalogSku next = change.apply(current);
         if (next == current || saveSkuPort.saveIfUnchanged(next)) {
            // Also for an event that changed nothing: a delivery that failed after the save is retried by the broker,
            // and its refresh must not be lost. A SKU whose product is not known yet (stock before the SKU) is picked
            // up when its SKU event arrives.
            if (next.productId() != null) {
               refreshProduct(next.productId());
            }
            return;
         }
      }
      throw new IllegalStateException("SKU " + skuId + " keeps changing; giving up after " + MAX_ATTEMPTS + " attempts");
   }

   private void refreshProduct(String productId) {
      updateProduct(productId, product -> product.withSkus(loadSkuPort.loadByProductId(productId)));
   }
}
