package com.ecom.shared.messaging;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bidirectional thread-safe registry and cache mapping {@link DomainEvent} classes
 * to and from their canonical {@link DomainEventSignature} (e.g., "Product.Created").
 */
@Lazy
@Component
public class DomainEventRegistry {

   private final Map<Class<? extends DomainEvent>, DomainEventSignature> clazzToSignature = new ConcurrentHashMap<>();
   private final Map<String, Class<? extends DomainEvent>> signatureToClazz = new ConcurrentHashMap<>();

   /**
   * Pre-registers a domain event class for bidirectional lookup.
   *
   * @param clazz the domain event class to register
   */
   public void register(Class<? extends DomainEvent> clazz) {
      getSignature(clazz);
   }

   /**
   * Resolves the canonical signature (e.g., "Product.Created") from a DomainEvent class.
   * Automatically registers and caches the class if not already registered.
   *
   * @param clazz the domain event class
   * @return the resolved {@link DomainEventSignature}
   */
   public DomainEventSignature getSignature(Class<? extends DomainEvent> clazz) {
      Objects.requireNonNull(clazz, "Domain event class must not be null");
      return clazzToSignature.computeIfAbsent(clazz, this::computeAndRegister);
   }

   /**
   * Resolves the canonical signature (e.g., "Product.Created") from a DomainEvent instance.
   * Automatically registers and caches the class if not already registered.
   *
   * @param event the domain event instance
   * @return the resolved {@link DomainEventSignature}
   */
   public DomainEventSignature getSignature(DomainEvent event) {
      Objects.requireNonNull(event, "Domain Event must not be null");
      return getSignature(event.getClass());
   }

   /**
   * Resolves the DomainEvent class from its canonical dot-delimited signature.
   *
   * @param signature the dot-delimited signature string (e.g., "Product.Created")
   * @return the registered DomainEvent class, or {@code null} if unknown
   */
   public Class<? extends DomainEvent> getClazz(String signature) {
      Objects.requireNonNull(signature, "Signature must not be null");
      Class<? extends DomainEvent> clazz = signatureToClazz.get(signature);
      if (clazz != null) {
         return clazz;
      }
      String[] parts = signature.split("\\.", 2);
      if (parts.length == 2) {
         String normalized = new DomainEventSignature(parts[0], parts[1]).toString();
         return signatureToClazz.get(normalized);
      }
      return null;
   }

   /**
     * Resolves the DomainEvent class from its {@link DomainEventSignature}.
     *
     * @param signature the domain event signature
     * @return the registered DomainEvent class, or {@code null} if unknown
     */
   public Class<? extends DomainEvent> getClazz(DomainEventSignature signature) {
      Objects.requireNonNull(signature, "Signature must not be null");
      return getClazz(signature.toString());
   }

   /**
     * Decomposes class into aggregate and action, and registers reverse mapping.
     */
   private DomainEventSignature computeAndRegister(Class<? extends DomainEvent> targetClazz) {
      validateClassStructure(targetClazz);

      String aggregate = targetClazz.getDeclaringClass().getSimpleName();
      String action = targetClazz.getSimpleName();

      DomainEventSignature signature = new DomainEventSignature(aggregate, action);

      // Register reverse mapping for both canonical kebab-case and original PascalCase
      signatureToClazz.put(signature.toString(), targetClazz);
      signatureToClazz.put(aggregate + "." + action, targetClazz);

      return signature;
   }

   private void validateClassStructure(Class<?> clazz) {
      if (clazz.isAnonymousClass() || clazz.isSynthetic()) {
         throw new IllegalArgumentException("DomainEvent cannot be an anonymous or synthetic class: " + clazz.getName());
      }
      if (clazz.getDeclaringClass() == null) {
         throw new IllegalArgumentException("DomainEvent must be nested in its aggregate interface (for example Product.Created): " + clazz.getName());
      }
   }
}
