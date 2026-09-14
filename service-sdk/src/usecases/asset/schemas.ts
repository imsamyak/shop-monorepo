import { z } from 'zod';

/** One to ten image files (JPEG, PNG, GIF or WebP, 5 MB each); the backend reads the type from the file itself. */
export const uploadImagesBody = z.strictObject({
  files: z.array(z.instanceof(Blob)).min(1).max(10),
});

export const deleteImageQuery = z.strictObject({ key: z.string().min(1).max(300) });
