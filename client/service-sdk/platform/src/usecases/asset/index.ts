import { useCase } from '@shop/sdk-core';
import { deleteImageQuery, uploadImagesBody } from './schemas.js';
import type { UploadedImagesResponse } from './types.js';

export type * from './types.js';

/** Sellers and admins. The returned `items` come back in the order of `files`. */
export const uploadImages = useCase('UploadImages')
  .service('asset')
  .post('/api/assets/images')
  .body(uploadImagesBody)
  .multipart()
  .authRequired()
  .returns<UploadedImagesResponse>()
  .build();

/** Only the user who uploaded the image, or an admin. */
export const deleteImage = useCase('DeleteImage')
  .service('asset')
  .delete('/api/assets/images')
  .query(deleteImageQuery)
  .authRequired()
  .build();

export const assetUseCases = [uploadImages, deleteImage] as const;
