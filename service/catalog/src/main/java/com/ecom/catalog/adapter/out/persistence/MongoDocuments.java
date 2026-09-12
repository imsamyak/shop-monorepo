package com.ecom.catalog.adapter.out.persistence;

import com.ecom.catalog.domain.entity.Availability;
import com.ecom.catalog.domain.entity.CatalogProduct;
import com.ecom.catalog.domain.entity.CatalogSku;
import com.ecom.catalog.domain.entity.SkuSummary;
import org.bson.Document;
import org.bson.types.Decimal128;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The shape of the {@code products} and {@code skus} documents. Prices are stored as decimals so they sort as numbers.
 * {@code visible} and {@code listPrice} exist only for the home page queries and their indexes.
 */
final class MongoDocuments {

   private MongoDocuments() {
   }

   static Document toDocument(CatalogProduct product, long revision) {
      return new Document("_id", product.id())
            .append("revision", revision)
            .append("version", product.version())
            .append("created", product.created())
            .append("deleted", product.deleted())
            .append("visible", product.visible())
            .append("sellerId", product.sellerId())
            .append("title", product.title())
            .append("description", product.description())
            .append("images", product.images())
            .append("price", decimal(product.price()))
            .append("createdAt", product.createdAt() == null ? null : Date.from(product.createdAt()))
            .append("skus", product.skus().stream().map(MongoDocuments::toDocument).toList())
            .append("lowestSkuPrice", decimal(product.lowestSkuPrice()))
            .append("listPrice", decimal(product.listPrice()))
            .append("availability", product.availability().name());
   }

   static CatalogProduct toProduct(Document document) {
      Date createdAt = document.getDate("createdAt");
      return CatalogProduct.builder()
            .id(document.getString("_id"))
            .revision(document.getLong("revision"))
            .version(document.getLong("version"))
            .created(document.getBoolean("created", false))
            .deleted(document.getBoolean("deleted", false))
            .sellerId(document.getString("sellerId"))
            .title(document.getString("title"))
            .description(document.getString("description"))
            .images(List.copyOf(document.getList("images", String.class, List.of())))
            .price(decimal(document.get("price")))
            .createdAt(createdAt == null ? null : createdAt.toInstant())
            .skus(document.getList("skus", Document.class, List.of()).stream().map(MongoDocuments::toSummary).toList())
            .lowestSkuPrice(decimal(document.get("lowestSkuPrice")))
            .availability(Availability.valueOf(document.getString("availability")))
            .build();
   }

   static Document toDocument(CatalogSku sku, long revision) {
      return new Document("_id", sku.skuId())
            .append("revision", revision)
            .append("productId", sku.productId())
            .append("version", sku.version())
            .append("deleted", sku.deleted())
            .append("price", decimal(sku.price()))
            .append("properties", new Document(new LinkedHashMap<String, Object>(sku.properties())))
            .append("stockVersion", sku.stockVersion())
            .append("availableStock", sku.availableStock());
   }

   static CatalogSku toSku(Document document) {
      return CatalogSku.builder()
            .skuId(document.getString("_id"))
            .productId(document.getString("productId"))
            .revision(document.getLong("revision"))
            .version(document.getLong("version"))
            .deleted(document.getBoolean("deleted", false))
            .price(decimal(document.get("price")))
            .properties(properties(document.get("properties", Document.class)))
            .stockVersion(document.getLong("stockVersion"))
            .availableStock(document.getInteger("availableStock", 0))
            .build();
   }

   private static Document toDocument(SkuSummary sku) {
      return new Document("skuId", sku.skuId())
            .append("price", decimal(sku.price()))
            .append("properties", new Document(new LinkedHashMap<String, Object>(sku.properties())))
            .append("availableStock", sku.availableStock());
   }

   private static SkuSummary toSummary(Document document) {
      return new SkuSummary(document.getString("skuId"), decimal(document.get("price")),
            properties(document.get("properties", Document.class)), document.getInteger("availableStock", 0));
   }

   private static Map<String, String> properties(Document document) {
      Map<String, String> properties = new LinkedHashMap<>();
      if (document != null) {
         document.forEach((key, value) -> properties.put(key, String.valueOf(value)));
      }
      return Collections.unmodifiableMap(properties);
   }

   private static Decimal128 decimal(BigDecimal value) {
      return value == null ? null : new Decimal128(value);
   }

   private static BigDecimal decimal(Object value) {
      return value == null ? null : ((Decimal128) value).bigDecimalValue();
   }
}
