package com.ecom.catalog.adapter.out.persistence;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Creates the indexes the home page queries need when the service starts; creating one that exists is harmless. Every
 * query filters on {@code visible}, optionally on {@code availability}, and sorts by creation time or price (the id
 * breaks ties), so each combination has an index that is read forwards or backwards.
 */
@Slf4j
@Component
public class MongoIndexes implements ApplicationRunner {

   private final MongoDatabase database;

   public MongoIndexes(MongoDatabase database) {
      this.database = database;
   }

   @Override
   public void run(ApplicationArguments args) {
      createIndexes();
   }

   void createIndexes() {
      var products = database.getCollection(ProductMongoAdapter.COLLECTION);
      products.createIndex(Indexes.ascending("visible", "createdAt", "_id"));
      products.createIndex(Indexes.ascending("visible", "listPrice", "_id"));
      products.createIndex(Indexes.ascending("visible", "availability", "createdAt", "_id"));
      products.createIndex(Indexes.ascending("visible", "availability", "listPrice", "_id"));
      database.getCollection(SkuMongoAdapter.COLLECTION).createIndex(Indexes.ascending("productId"));
      log.info("Indexes ensured on {}", database.getName());
   }
}
