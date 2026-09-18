package com.ecom.shared.dto.result;

import java.util.Iterator;
import java.util.List;

/**
 * Root boundary contract for use-case results.
 * Every result is either a {@link CommandResult} or a {@link QueryResult}.
 * The shapes below are shared where both sides allow them.
 */
public sealed interface Result permits CommandResult, QueryResult {

   /**
    * Single-item result. Allowed for commands and queries.
    */
   non-sealed interface One extends CommandResult, QueryResult {

   }

   /**
    * Unpaginated collection. Allowed for commands and queries.
    */
   non-sealed interface Many<T> extends CommandResult, QueryResult, Iterable<T> {
      List<T> items();

      @Override
      default Iterator<T> iterator() {
         return items().iterator();
      }
   }

   /**
    * Cursor-paginated collection. Queries only.
    */
   non-sealed interface Page<T> extends QueryResult {
      List<T> items();

      String cursor();
   }

   /**
    * Empty result for a successful side effect. Commands only.
    */
   non-sealed interface None extends CommandResult {

   }
}
