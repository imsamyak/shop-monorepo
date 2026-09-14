/** Types of response fields, as the backend's Jackson mapper writes them. */

export type Uuid = string;

/** `Instant`, written as ISO-8601 text. */
export type IsoInstant = string;

/** `BigDecimal`, written as a JSON number. */
export type Decimal = number;

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
