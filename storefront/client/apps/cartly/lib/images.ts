/** Shown when a product has no pictures. */
export const PLACEHOLDER_IMAGE = '/placeholder.svg';

/** The main picture of a product: the first of its images, which are in the order the seller gave them. */
export function mainImage(images: readonly string[] | undefined): string {
  return images?.[0] ?? PLACEHOLDER_IMAGE;
}
