package com.ecom.checkout.port.out.client.inventory;

import java.util.Collection;
import java.util.UUID;

public interface ReleaseStockPort {

   /** Releases the reservations atomically. Idempotent: already released or expired reservations are accepted. */
   void release(Collection<UUID> reservationIds);
}
