import { z } from 'zod';

/** Field rules shared across services, matching the backend's bean validation. */

export const uuid = z.uuid();

/** A database id such as a SKU id (`Long` on the backend). */
export const id = z.number().int().positive();

/** Optimistic-lock version the caller last saw. */
export const version = z.number().int().nonnegative();

/** `@NotBlank` plus `@Size`. Not trimmed, so passwords are sent as typed. */
export const requiredText = (max: number, min = 1) => z.string().regex(/\S/, 'must not be blank').min(min).max(max);

/** `@MonetaryAmount` / `@DecimalMin`: a JSON number, as the backend reads `BigDecimal`. */
export const money = (min = 0) => z.number().min(min);

export const page = z.number().int().nonnegative();

/** Page size, `@Min(1) @Max(100)` in every service. */
export const size = z.number().int().min(1).max(100);
