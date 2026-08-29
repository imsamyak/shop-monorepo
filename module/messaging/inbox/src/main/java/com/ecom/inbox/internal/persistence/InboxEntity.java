package com.ecom.inbox.internal.persistence;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing an ingested message in the transactional inbox.
 * Guarantees idempotency via unique message_id constraint and supports TTL eviction.
 */
@Entity
@Table(name = "inbox", indexes = {
      @Index(name = "idx_inbox_message_id", columnList = "message_id", unique = true),
      @Index(name = "idx_inbox_expires_at", columnList = "expires_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InboxEntity {

   @Id
   @GeneratedValue(strategy = GenerationType.UUID)
   @Column(name = "id", updatable = false, nullable = false)
   private UUID id;

   @NotNull
   @Size(max = 100)
   @Column(name = "message_id", nullable = false, unique = true, length = 100)
   private String messageId;

   @NotNull
   @Size(max = 100)
   @Column(name = "signature", nullable = false, length = 100)
   private String signature;

   @NotNull
   @Column(name = "received_at", nullable = false)
   private Instant receivedAt;

   @NotNull
   @Column(name = "expires_at", nullable = false)
   private Instant expiresAt;
}
