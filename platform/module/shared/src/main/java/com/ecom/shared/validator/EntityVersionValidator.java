package com.ecom.shared.validator;

import com.ecom.shared.entity.DomainEntity;
import com.ecom.shared.exception.conflict.VersionConflictException;

/**
 * Compares the version a client last saw with the entity's current version and fails with a 409
 * ({@link VersionConflictException}) when they differ.
 */
public class EntityVersionValidator {

   public void validate(DomainEntity entity, long requestedVersion) {
      if (entity == null) {
         throw new IllegalArgumentException("Entity cannot be null for version validation");
      }
      if (entity.version() != requestedVersion) {
         throw VersionConflictException.of(entity.getEntityName(), requestedVersion, entity.version());
      }
   }
}
