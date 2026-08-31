package com.ecom.listing.port.out.persistence.sku;

/**
 * Load-for-update plus save: SKUs are soft-deleted, so deleting is a state change followed by a save.
 */
public interface DeleteSkuPort extends LoadSkuForUpdatePort, SaveSkuPort {
}
