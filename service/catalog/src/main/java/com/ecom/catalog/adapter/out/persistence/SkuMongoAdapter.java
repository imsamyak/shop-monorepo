package com.ecom.catalog.adapter.out.persistence;

import com.ecom.catalog.domain.entity.CatalogSku;
import com.ecom.catalog.port.out.persistence.LoadSkuPort;
import com.ecom.catalog.port.out.persistence.SaveSkuPort;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The {@code skus} collection: what is known of each SKU and its stock, whether or not its product exists yet. Saves
 * work like the product's.
 */
@Component
public class SkuMongoAdapter implements LoadSkuPort, SaveSkuPort {

   static final String COLLECTION = "skus";

   private final MongoCollection<Document> skus;

   public SkuMongoAdapter(MongoDatabase database) {
      this.skus = database.getCollection(COLLECTION);
   }

   @Override
   public Optional<CatalogSku> loadBySkuId(String skuId) {
      return Optional.ofNullable(skus.find(Filters.eq("_id", skuId)).first()).map(MongoDocuments::toSku);
   }

   @Override
   public List<CatalogSku> loadByProductId(String productId) {
      return skus.find(Filters.eq("productId", productId)).map(MongoDocuments::toSku).into(new ArrayList<>());
   }

   @Override
   public boolean saveIfUnchanged(CatalogSku sku) {
      Document document = MongoDocuments.toDocument(sku, sku.revision() + 1);
      if (sku.revision() == 0) {
         try {
            skus.insertOne(document);
            return true;
         } catch (MongoWriteException e) {
            if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
               return false;
            }
            throw e;
         }
      }
      return skus.replaceOne(
            Filters.and(Filters.eq("_id", sku.skuId()), Filters.eq("revision", sku.revision())),
            document).getMatchedCount() == 1;
   }
}
