package com.ecom.listing.port.out.persistence.product;

/**
 * Load-for-update plus save: products are soft-deleted, so deleting is a state change followed by a save.
 */
public interface DeleteProductPort extends LoadProductForUpdatePort, SaveProductPort {
}
