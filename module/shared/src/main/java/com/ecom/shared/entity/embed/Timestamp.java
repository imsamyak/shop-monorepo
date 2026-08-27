package com.ecom.shared.entity.embed;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDateTime;

/**
 * Created/modified timestamps, embedded into an entity's own table
 * (see {@link com.ecom.shared.entity.DomainEntity}).
 */
@Embeddable
public class Timestamp {

   /**
    * Set once on the first save and never changed afterwards.
    */
   @Column(name = "created_at", nullable = false, updatable = false)
   public LocalDateTime createdAt;

   /**
    * Set on every save.
    */
   @Column(name = "modified_at", nullable = false)
   public LocalDateTime modifiedAt;

   /**
    * Stamps the current time: fills {@code createdAt} if it is still empty
    * and always updates {@code modifiedAt}. Both get the exact same value on the first call.
    */
   public void refresh() {

      LocalDateTime now = LocalDateTime.now();
      if (createdAt == null) {
         createdAt = now;
      }

      modifiedAt = now;
   }
}
