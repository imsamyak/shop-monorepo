package com.ecom.asset;

/**
 * Where uploaded files are kept. The module only asks for these three things; the technology behind them (S3 or
 * anything else) is chosen by the service through {@code ecom.asset.client}.
 */
public interface AssetStorage {

   /**
    * Stores the content under the key, replacing anything already there, so that it can be read by anyone at
    * {@link #publicUrl(String)}.
    *
    * @throws RuntimeException if the content could not be stored
    */
   void put(String key, byte[] content, String contentType);

   /**
    * Removes the file. Removing a key that is not there is not an error.
    */
   void delete(String key);

   /**
    * The address anybody can fetch the file from.
    */
   String publicUrl(String key);
}
