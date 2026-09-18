package com.ecom.inbox.internal.clean;

import com.ecom.inbox.internal.persistence.InboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InboxCleanerTest {

   private InboxRepository repository;
   private DefaultInboxCleaner cleaner;

   @BeforeEach
   void setUp() {
      repository = mock(InboxRepository.class);
      PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
      cleaner = new DefaultInboxCleaner(repository, new TransactionTemplate(txManager));
   }

   @Test
   @DisplayName("Deletes expired inbox messages and returns count")
   void deletesExpiredMessages() {
      when(repository.deleteExpiredMessages(any(Instant.class))).thenReturn(5);

      int count = cleaner.cleanup();

      assertEquals(5, count);
      verify(repository).deleteExpiredMessages(any(Instant.class));
   }

   @Test
   @DisplayName("Handles repository exception gracefully returning 0")
   void handlesExceptionGracefully() {
      when(repository.deleteExpiredMessages(any(Instant.class))).thenThrow(new RuntimeException("DB error"));

      int count = cleaner.cleanup();

      assertEquals(0, count);
   }
}
