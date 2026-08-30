package com.ecom.shared.dto.patch;

import org.openapitools.jackson.nullable.JsonNullable;

import java.io.Serializable;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents a tri-state patch field for partial updates:
 * <ul>
 *     <li><b>Undefined</b> (omitted): {@link #undefined()} - field should not be updated.</li>
 *     <li><b>Specified as null</b>: {@link #of(Object)} with {@code null} - field should be cleared in the database.</li>
 *     <li><b>Specified with value</b>: {@link #of(Object)} with a non-null value - field should be set to the new value.</li>
 * </ul>
 *
 * @param <T> the wrapped value type
 */
public record Patch<T>(boolean isSpecified, T value) implements Serializable {

   private static final Patch<?> UNDEFINED = new Patch<>(false, null);

   public Patch {
      if (!isSpecified) {
         value = null;
      }
   }

   /**
     * Creates an undefined patch representing an omitted or untouched field.
     */
   @SuppressWarnings("unchecked")
   public static <T> Patch<T> undefined() {
      return (Patch<T>) UNDEFINED;
   }

   /**
     * Creates a specified patch with the given value (which may be null to indicate clearing).
     */
   public static <T> Patch<T> of(T value) {
      return new Patch<>(true, value);
   }

   /**
     * Converts an {@link org.openapitools.jackson.nullable.JsonNullable} to a {@link Patch}.
     */
   public static <T> Patch<T> from(org.openapitools.jackson.nullable.JsonNullable<T> jsonNullable) {
      if (jsonNullable == null || !jsonNullable.isPresent()) {
         return undefined();
      }
      return of(jsonNullable.get());
   }

   /**
     * Returns true if the field was omitted / undefined.
     */
   public boolean isUndefined() {
      return !isSpecified;
   }

   /**
     * Executes the given consumer if the field was explicitly specified.
     * The consumer will be passed {@code null} if the field was specified as null.
     */
   public void ifSpecified(Consumer<? super T> consumer) {
      if (isSpecified) {
         consumer.accept(value);
      }
   }

   /**
     * Maps the value if specified, preserving undefined state.
     */
   public <U> Patch<U> map(Function<? super T, ? extends U> mapper) {
      if (!isSpecified) {
         return undefined();
      }
      return Patch.of(mapper.apply(value));
   }

   /**
     * Converts to an Optional (empty if undefined or if value is null).
     */
   public Optional<T> toOptional() {
      return Optional.ofNullable(value);
   }

   public T orElse(T other) {
      return isSpecified ? value : other;
   }

   public T orElseGet(Supplier<? extends T> supplier) {
      return isSpecified ? value : supplier.get();
   }

   /**
     * Converts this {@link Patch} to an {@link JsonNullable}.
     */
   public JsonNullable<T> toJsonNullable() {
      if (!isSpecified) {
         return JsonNullable.undefined();
      }
      return JsonNullable.of(value);
   }
}
