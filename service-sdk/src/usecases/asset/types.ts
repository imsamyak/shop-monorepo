export interface UploadedImage {
  /** `images/<userId>/<random>.<extension>`; pass it to `deleteImage`. */
  key: string;
  /** The public address of the image; put this in a product's `images`. */
  url: string;
  contentType: 'image/jpeg' | 'image/png' | 'image/gif' | 'image/webp';
  /** Bytes. */
  size: number;
}

/** In the order the files were sent. */
export interface UploadedImagesResponse {
  items: UploadedImage[];
}
