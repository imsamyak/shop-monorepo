package com.ecom.outbox.internal.persistence;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing an outbox event record stored in the {@code outbox_events} table.
 */
@Entity
@Table(
      name = "outbox_events",
      indexes = {
            @Index(name = "idx_outbox_status_created", columnList = "status, created_at"),
            @Index(name = "idx_outbox_claim", columnList = "status, next_retry_at, locked_until")
      }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEntity {

   @Id
   @GeneratedValue(strategy = GenerationType.UUID)
   @Column(name = "id", nullable = false, updatable = false)
   private UUID id;

   @Embedded
   @NotNull
   private OutboxEvent event;

   @NotNull
   @Enumerated(EnumType.STRING)
   @Column(name = "status", nullable = false, length = 20)
   @Builder.Default
   private OutboxStatus status = OutboxStatus.PENDING;

   @Column(name = "retry_count", nullable = false)
   @Builder.Default
   private int retryCount = 0;

   @Column(name = "max_retries", nullable = false)
   @Builder.Default
   private int maxRetries = 5;

   @Column(name = "next_retry_at")
   private Instant nextRetryAt;

   @Column(name = "locked_until")
   private Instant lockedUntil;

   @Column(name = "last_error", columnDefinition = "TEXT")
   private String lastError;

   @NotNull
   @Column(name = "created_at", nullable = false, updatable = false)
   @Builder.Default
   private Instant createdAt = Instant.now();

   @Column(name = "processed_at")
   private Instant processedAt;

   public String getAggregateType() {
      return event != null ? event.getAggregateType() : null;
   }

   public String getAggregateId() {
      return event != null ? event.getAggregateId() : null;
   }

   public String getPayload() {
      return event != null ? event.getPayload() : null;
   }
}
