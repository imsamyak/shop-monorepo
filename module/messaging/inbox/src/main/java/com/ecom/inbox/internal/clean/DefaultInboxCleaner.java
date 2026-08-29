package com.ecom.inbox.internal.clean;

import com.ecom.inbox.InboxCleaner;
import com.ecom.inbox.internal.persistence.InboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

/**
 * Default implementation of {@link InboxCleaner}.
 * Purges expired messages whose TTL has elapsed.
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultInboxCleaner implements InboxCleaner {

   private final InboxRepository repository;
   private final TransactionTemplate transactionTemplate;

   @Override
   public int cleanup() {
      Instant now = Instant.now();
      try {
         Integer deleted = transactionTemplate.execute(status -> repository.deleteExpiredMessages(now));
         int count = deleted != null ? deleted : 0;
         if (count > 0) {
            log.info("InboxCleaner pruned {} expired inbox messages", count);
         }
         return count;
      } catch (Exception e) {
         log.warn("InboxCleaner failed to prune expired messages: {}", e.getMessage());
         return 0;
      }
   }
}
