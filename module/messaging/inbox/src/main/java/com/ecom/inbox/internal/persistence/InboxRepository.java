package com.ecom.inbox.internal.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link InboxEntity}.
 */
@Repository
public interface InboxRepository extends JpaRepository<InboxEntity, UUID> {

   boolean existsByMessageId(String messageId);

   Optional<InboxEntity> findByMessageId(String messageId);

   @Modifying
   @Query("DELETE FROM InboxEntity i WHERE i.expiresAt < :now")
   int deleteExpiredMessages(@Param("now") Instant now);
}
