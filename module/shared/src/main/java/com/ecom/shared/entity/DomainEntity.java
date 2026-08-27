package com.ecom.shared.entity;

import com.ecom.shared.entity.embed.Timestamp;
import jakarta.persistence.*;

/**
 * Base class for every persisted domain entity.
 * Its fields become columns on the table of each subclass; it has no table of its own.
 * Provides optimistic locking, soft delete and created/modified timestamps.
 */
@MappedSuperclass
public abstract class DomainEntity {

   /**
    * Optimistic-locking counter. JPA increments it on every update and rejects
    * a write made from a stale copy of the row.
    */
   @Version
   private long version;

   /**
    * Soft-delete flag. The row stays in the table and is only marked as deleted.
    */
   @Column(name = "is_deleted", nullable = false)
   private boolean isDeleted = false;

   /**
    * Created/modified timestamps, kept up to date by the lifecycle callbacks below.
    */
   @Embedded
   private Timestamp ts = new Timestamp();

   public final long version() {
      return this.version;
   }

   public final boolean isDeleted(){
      return this.isDeleted;
   }

   /**
    * Soft-deletes this entity.
    *
    * @return {@code true} if it was deleted now, {@code false} if it was already deleted
    */
   public final boolean markAsDeleted() {

      if (isDeleted) {
         return false;
      }

      isDeleted = true;
      return true;
   }

   /**
    * Name of the entity as shown in errors, for example in a version conflict.
    */
   public abstract String getEntityName();

   /**
    * Sets both timestamps just before the first insert.
    */
   @PrePersist
   private void _beforeSave() {
      ts.refresh();
   }

   /**
    * Updates the modified timestamp just before every update.
    */
   @PreUpdate
   private void _beforeUpdate() {
      ts.refresh();
   }
}
