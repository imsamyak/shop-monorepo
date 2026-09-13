package com.ecom.checkout.port.out.client.inventory;

import java.util.Collection;
import java.util.UUID;

public interface CommitStockPort {

   /** Commits the reservations atomically. Idempotent: already committed reservations are accepted. */
   void commit(Collection<UUID> reservationIds);
}
