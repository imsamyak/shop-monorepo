package com.ecom.catalog.domain.entity;

import com.ecom.contract.product.Product;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * One product of the home page, built from {@code Product.*} events and the SKUs belonging to it. SKU events can
 * arrive before the product, so a record may exist before {@code Product.Created} was seen ({@code created} false); it
 * is not shown until then. Every {@code apply...} / {@code with...} method returns {@code this} when the event changes
 * nothing (duplicate, older than what is stored, or after the product was deleted).
 *
 * @param revision       how often this record was stored; the store only accepts a save that was based on the latest one
 * @param version        the product's version of the last applied {@code Product.Created/Updated}, null before any
 * @param created        {@code Product.Created} was received
 * @param deleted        the product was deleted; final, a late older event cannot bring it back
 * @param images         the picture addresses in display order (the first is the main one), from the last applied
 *                       {@code Product.Created/Updated}
 * @param price          the product's default price, which no event changes after creation
 * @param createdAt      when the catalog first received {@code Product.Created}; never overwritten
 * @param skus           the live SKUs, derived from the SKU records
 * @param lowestSkuPrice the lowest price among {@code skus}, null when there are none
 * @param availability   {@link Availability#AVAILABLE} when at least one live SKU has stock, derived like {@code skus}
 */
@Builder(toBuilder = true)
public record CatalogProduct(
      String id,
      long revision,
      Long version,
      boolean created,
      boolean deleted,
      String sellerId,
      String title,
      String description,
      List<String> images,
      BigDecimal price,
      Instant createdAt,
      List<SkuSummary> skus,
      BigDecimal lowestSkuPrice,
      Availability availability
) {

   public static CatalogProduct empty(String id) {
      return CatalogProduct.builder().id(id).images(List.of()).skus(List.of()).availability(Availability.OUT_OF_STOCK).build();
   }

   /**
    * The seller, price and creation time are fixed by the first {@code Created} received, even when a newer
    * {@code Updated} was applied before it; the text follows the version rule.
    */
   public CatalogProduct applyCreated(Product.Created event, Instant now) {
      if (deleted) {
         return this;
      }
      CatalogProduct next = this;
      if (!created) {
         next = next.toBuilder().created(true).sellerId(event.sellerId()).price(event.defaultPrice()).createdAt(now).build();
      }
      return next.applyUpdated(event.title(), event.description(), event.images(), event.version());
   }

   public CatalogProduct applyUpdated(String title, String description, List<String> images, long version) {
      if (deleted || (this.version != null && version <= this.version)) {
         return this;
      }
      return toBuilder().title(title).description(description).images(images == null ? List.of() : List.copyOf(images))
            .version(version).build();
   }

   /**
    * Final whatever the version.
    */
   public CatalogProduct applyDeleted() {
      return deleted ? this : toBuilder().deleted(true).build();
   }

   /**
    * Recomputes everything derived from the SKU records of this product.
    */
   public CatalogProduct withSkus(Collection<CatalogSku> records) {
      List<SkuSummary> live = records.stream()
            .filter(sku -> !sku.deleted() && sku.version() != null)
            .sorted(Comparator.comparing(CatalogSku::skuId))
            .map(sku -> new SkuSummary(sku.skuId(), sku.price(), sku.properties(), sku.availableStock()))
            .toList();
      BigDecimal lowest = live.stream().map(SkuSummary::price).filter(java.util.Objects::nonNull)
            .min(Comparator.naturalOrder()).orElse(null);
      boolean anyInStock = live.stream().anyMatch(sku -> sku.availableStock() > 0);
      CatalogProduct next = toBuilder()
            .skus(live)
            .lowestSkuPrice(lowest)
            .availability(anyInStock ? Availability.AVAILABLE : Availability.OUT_OF_STOCK)
            .build();
      return next.equals(this) ? this : next;
   }

   /**
    * Shown on the home page: known from its {@code Created} event and not deleted.
    */
   public boolean visible() {
      return created && !deleted;
   }

   /**
    * The price the list is sorted by: the cheapest SKU, or the product's own price while it has none.
    */
   public BigDecimal listPrice() {
      return lowestSkuPrice != null ? lowestSkuPrice : price;
   }
}
