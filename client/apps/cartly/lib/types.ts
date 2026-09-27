/** What the app's own `/api` routes answer. Shared by the route handlers (server) and the store (browser). */

export const SORTS = {
  'createdAt,desc': 'Newest',
  'price,asc': 'Price: low to high',
  'price,desc': 'Price: high to low',
} as const;

export type Sort = keyof typeof SORTS;

export interface ProductQuery {
  search: string;
  sort: Sort;
  inStock: boolean;
  page: number;
  size: number;
}

export interface ProductCard {
  id: string;
  title: string;
  image: string;
  /** The lowest price the product is sold at. */
  price: number;
  /** True when other options cost more than `price`. */
  from: boolean;
  available: boolean;
}

export interface ProductPage {
  items: ProductCard[];
  page: number;
  totalPages: number;
  totalElements: number;
}

export interface CartLine {
  skuId: number;
  quantity: number;
  productId: string | null;
  title: string;
  image: string;
  /** The option, e.g. `UK 9` or `128 GB`. */
  label: string;
  unitPrice: number;
  available: boolean;
}

export interface CartView {
  version: number;
  /** Units in the cart, for the badge. */
  count: number;
  /** An estimate from catalog prices; checkout prices it for real. */
  total: number;
  items: CartLine[];
}
