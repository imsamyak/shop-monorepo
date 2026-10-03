// Product photos: the files in scripts/resources/images, named in products.json.

import { readFileSync } from 'node:fs';
import { join } from 'node:path';

const RESOURCES = join(import.meta.dirname, '../../../resources');

const TYPES = { png: 'image/png', jpg: 'image/jpeg', jpeg: 'image/jpeg', webp: 'image/webp' };

/** The photo at `path` (relative to resources/) as a `File`, ready to upload. */
export function picture(path) {
  const extension = path.split('.').pop().toLowerCase();
  const type = TYPES[extension];
  if (!type) throw new Error(`${path}: only png, jpg and webp photos are supported`);
  return new File([readFileSync(join(RESOURCES, path))], path.split('/').pop(), { type });
}
