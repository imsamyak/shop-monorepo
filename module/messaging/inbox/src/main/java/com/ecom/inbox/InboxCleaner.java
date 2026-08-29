package com.ecom.inbox;

/**
 * Strategy contract for purging expired messages from the inbox table.
 */
public interface InboxCleaner {

   /**
     * Purges expired messages from the inbox.
     *
     * @return the number of deleted records
     */
   int cleanup();
}
