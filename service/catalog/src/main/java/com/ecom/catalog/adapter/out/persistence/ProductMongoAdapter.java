package com.ecom.catalog.adapter.out.persistence;

import com.ecom.catalog.domain.entity.Availability;
import com.ecom.catalog.domain.entity.CatalogProduct;
import com.ecom.catalog.domain.entity.ProductSort;
import com.ecom.catalog.port.out.persistence.LoadProductPort;
import com.ecom.catalog.port.out.persistence.QueryProductsPort;
import com.ecom.catalog.port.out.persistence.SaveProductPort;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The {@code products} collection. A save is a single insert or a single replace that matches on the revision it was
 * based on, which is what makes concurrent and repeated deliveries safe.
 */
@Component
public class ProductMongoAdapter implements LoadProductPort, SaveProductPort, QueryProductsPort {

   static final String COLLECTION = "products";

   private final MongoCollection<Document> products;

   public ProductMongoAdapter(MongoDatabase database) {
      this.products = database.getCollection(COLLECTION);
   }

   @Override
   public Optional<CatalogProduct> loadById(String productId) {
      return Optional.ofNullable(products.find(Filters.eq("_id", productId)).first()).map(MongoDocuments::toProduct);
   }

   @Override
   public boolean saveIfUnchanged(CatalogProduct product) {
      Document document = MongoDocuments.toDocument(product, product.revision() + 1);
      if (product.revision() == 0) {
         try {
            products.insertOne(document);
            return true;
         } catch (MongoWriteException e) {
            if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
               return false;
            }
            throw e;
         }
      }
      return products.replaceOne(
            Filters.and(Filters.eq("_id", product.id()), Filters.eq("revision", product.revision())),
            document).getMatchedCount() == 1;
   }

   @Override
   public Optional<CatalogProduct> findVisibleById(String productId) {
      return Optional.ofNullable(products.find(Filters.and(Filters.eq("_id", productId), Filters.eq("visible", true)))
            .first()).map(MongoDocuments::toProduct);
   }

   @Override
   public Found findVisible(Availability availability, ProductSort sort, int page, int size) {
      List<Bson> conditions = new ArrayList<>();
      conditions.add(Filters.eq("visible", true));
      if (availability != null) {
         conditions.add(Filters.eq("availability", availability.name()));
      }
      Bson filter = Filters.and(conditions);

      List<CatalogProduct> found = products.find(filter)
            .sort(order(sort))
            .skip(page * size)
            .limit(size)
            .map(MongoDocuments::toProduct)
            .into(new ArrayList<>());
      return new Found(found, products.countDocuments(filter));
   }

   /**
    * The product id breaks ties in the direction of the sort, so every page continues where the last one ended.
    */
   private static Bson order(ProductSort sort) {
      return switch (sort) {
         case NEWEST -> Sorts.descending("createdAt", "_id");
         case OLDEST -> Sorts.ascending("createdAt", "_id");
         case PRICE_ASC -> Sorts.ascending("listPrice", "_id");
         case PRICE_DESC -> Sorts.descending("listPrice", "_id");
      };
   }
}
