package com.ecom.infra.adapter.inbox;

import com.ecom.inbox.InboxListener;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The message layout every transport carries: {@code {"aggregate": ..., "action": ..., "data": {...}}}. Turns one into
 * an inbox call. A message that cannot be read is an {@link IllegalArgumentException}, which no retry can fix.
 */
final class InboxEnvelopes {

   private InboxEnvelopes() {
   }

   static void dispatch(InboxListener listener, ObjectMapper objectMapper, String messageId, String json, String location) {
      JsonNode envelope;
      try {
         envelope = objectMapper.readTree(json);
      } catch (Exception e) {
         throw new IllegalArgumentException("Unreadable message at " + location, e);
      }
      String aggregate = envelope.path("aggregate").asText("");
      String action = envelope.path("action").asText("");
      JsonNode data = envelope.get("data");
      if (aggregate.isBlank() || action.isBlank() || data == null || data.isNull()) {
         throw new IllegalArgumentException("Malformed envelope at " + location);
      }
      listener.listen(messageId, aggregate + "." + action, data.toString());
   }
}
