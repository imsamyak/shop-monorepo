'use server';

import { SdkHttpError, type ProductResponse } from '@shop/platform-sdk';
import { revalidatePath } from 'next/cache';
import { redirect } from 'next/navigation';
import { errorMessage } from './errors';
import type { FormState } from './actions';
import { api } from './sdk';
import { requireSeller } from './session';

const MAX_IMAGES = 10;

/** Only same-site paths, so `?next=` cannot send the seller elsewhere. */
function safeNext(value: FormDataEntryValue | null): string {
  const next = typeof value === 'string' ? value : '';
  return next.startsWith('/') && !next.startsWith('//') ? next : '/seller';
}

/** The non-empty files of a file input (a browser sends one empty part when nothing was chosen). */
function filesOf(formData: FormData, name: string): File[] {
  return formData.getAll(name).filter((entry): entry is File => entry instanceof File && entry.size > 0);
}

/** Stores the files and returns their public addresses, in the order the files were chosen. */
async function upload(files: File[]): Promise<string[]> {
  if (files.length === 0) return [];
  const { items } = await api().assets.uploadImages({ files });
  return items.map((item) => item.url);
}

/** The storage key of an uploaded image, from its address: everything from `images/` on. */
function keyOf(url: string): string | null {
  const at = url.indexOf('/images/');
  return at >= 0 ? url.slice(at + 1) : null;
}

export async function registerSeller(_: FormState, formData: FormData): Promise<FormState> {
  const credentials = { email: String(formData.get('email') ?? ''), password: String(formData.get('password') ?? '') };
  try {
    await api().auth.registerSeller(credentials);
  } catch (error) {
    if (error instanceof SdkHttpError && error.status === 409) {
      return { error: 'An account with this email already exists.' };
    }
    return { error: errorMessage(error) };
  }
  redirect(safeNext(formData.get('next')));
}

export async function createProduct(_: FormState, formData: FormData): Promise<FormState> {
  await requireSeller('/seller/products/new');
  const files = filesOf(formData, 'images');
  if (files.length > MAX_IMAGES) return { error: `At most ${MAX_IMAGES} pictures.` };

  let id: string;
  try {
    const images = await upload(files);
    ({ id } = await api().products.createProduct({
      title: String(formData.get('title') ?? '').trim(),
      description: String(formData.get('description') ?? '').trim(),
      defaultPrice: Number(formData.get('defaultPrice')),
      images,
    }));
  } catch (error) {
    return { error: errorMessage(error) };
  }
  revalidatePath('/seller');
  redirect(`/seller/products/${id}`);
}

/** Failures on the manage page are shown through `?error=`. */
async function change(productId: string, run: (product: ProductResponse) => Promise<unknown>) {
  const page = `/seller/products/${productId}`;
  await requireSeller(page);
  try {
    await run(await api().products.getProduct({ id: productId }));
  } catch (error) {
    redirect(`${page}?error=${encodeURIComponent(errorMessage(error))}`);
  }
  revalidatePath(page);
  revalidatePath('/', 'layout');
  redirect(page);
}

export async function saveDetails(formData: FormData) {
  const productId = String(formData.get('productId'));
  await change(productId, (product) =>
    api().products.updateProduct({
      id: productId,
      version: product.version,
      title: String(formData.get('title') ?? '').trim(),
      description: String(formData.get('description') ?? '').trim() || null,
    }),
  );
}

export async function addImages(formData: FormData) {
  const productId = String(formData.get('productId'));
  const files = filesOf(formData, 'images');
  await change(productId, async (product) => {
    if (files.length === 0) throw new Error('Choose at least one picture.');
    if (product.images.length + files.length > MAX_IMAGES) throw new Error(`At most ${MAX_IMAGES} pictures.`);
    const added = await upload(files);
    await api().products.updateProduct({
      id: productId,
      version: product.version,
      title: product.title,
      images: [...product.images, ...added],
    });
  });
}

/** Moves a picture one place up (`direction` -1) or down (+1); the first picture is the main one. */
export async function moveImage(formData: FormData) {
  const productId = String(formData.get('productId'));
  const from = Number(formData.get('position'));
  const to = from + Number(formData.get('direction'));
  await change(productId, async (product) => {
    if (from < 0 || to < 0 || from >= product.images.length || to >= product.images.length) return;
    const images = [...product.images];
    const [moved] = images.splice(from, 1);
    images.splice(to, 0, moved as string);
    await api().products.updateProduct({ id: productId, version: product.version, title: product.title, images });
  });
}

export async function removeImage(formData: FormData) {
  const productId = String(formData.get('productId'));
  const url = String(formData.get('url'));
  await change(productId, async (product) => {
    await api().products.updateProduct({
      id: productId,
      version: product.version,
      title: product.title,
      images: product.images.filter((image) => image !== url),
    });
    // The product no longer shows it; remove the stored file too, if it is ours. A failure here changes nothing for
    // the buyer, so it is ignored.
    const key = keyOf(url);
    if (key) await api().assets.deleteImage({ key }).catch(() => undefined);
  });
}

export async function addSku(formData: FormData) {
  const productId = String(formData.get('productId'));
  const name = String(formData.get('optionName') ?? '').trim();
  const value = String(formData.get('optionValue') ?? '').trim();
  const price = String(formData.get('price') ?? '').trim();
  await change(productId, () =>
    api().products.createSku({
      productId,
      ...(price ? { price: Number(price) } : {}),
      properties: name && value ? { [name]: value } : {},
    }),
  );
}

export async function addStock(formData: FormData) {
  const productId = String(formData.get('productId'));
  const skuId = Number(formData.get('skuId'));
  const quantity = Number(formData.get('quantity'));
  await change(productId, () => api().inventory.addStock({ skuId, quantity }));
}
