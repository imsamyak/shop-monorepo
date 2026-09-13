package com.ecom.outbox.internal.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Value Object representing the domain event payload stored inside an outbox entry.
 * Embedded directly into {@link OutboxEntity}.
 */
@Embeddable
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

   public static final int MAX_FIELD_LENGTH = 100;

   @NotNull
   @Size(max = MAX_FIELD_LENGTH)
   @Column(name = "aggregate_type", nullable = false, length = MAX_FIELD_LENGTH)
   private String aggregateType;

   @NotNull
   @Size(max = MAX_FIELD_LENGTH)
   @Column(name = "aggregate_id", nullable = false, length = MAX_FIELD_LENGTH)
   private String aggregateId;

   @NotNull
   @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
   private String payload;
}
