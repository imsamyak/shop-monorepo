package com.ecom.shared.exception.conflict;

import com.ecom.shared.exception.ErrorCode;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
public class VersionConflictException extends ConflictException {

   private final String entityName;
   private final long expectedVersion;
   private final long actualVersion;

   public VersionConflictException(String entityName, long expectedVersion, long actualVersion) {
      super(
          String.format("%s version conflict. Expected version %d, but found %d", entityName, expectedVersion, actualVersion),
          ErrorCode.VERSION_CONFLICT,
          Map.of(
              "entityName", entityName,
              "expectedVersion", expectedVersion,
              "actualVersion", actualVersion
          )
      );
      this.entityName = entityName;
      this.expectedVersion = expectedVersion;
      this.actualVersion = actualVersion;
   }

   @Builder
   public static VersionConflictException of(String entityName, long expectedVersion, long actualVersion) {
      return new VersionConflictException(entityName, expectedVersion, actualVersion);
   }
}
