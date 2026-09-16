'use server';

import { SdkHttpError } from '@shop/platform-sdk';
import { revalidatePath } from 'next/cache';
import { redirect } from 'next/navigation';
import { errorMessage } from './errors';
import { api } from './sdk';
import { getUser, requireUser } from './session';

export interface FormState {
  error?: string;
  message?: string;
}

/** Only same-site paths, so `?next=` cannot send the buyer elsewhere. */
function safeNext(value: FormDataEntryValue | null): string {
  const next = typeof value === 'string' ? value : '';
  return next.startsWith('/') && !next.startsWith('//') ? next : '/';
}

function credentials(formData: FormData) {
  return { email: String(formData.get('email') ?? ''), password: String(formData.get('password') ?? '') };
}

export async function login(_: FormState, formData: FormData): Promise<FormState> {
  try {
    await api().auth.login(credentials(formData));
  } catch (error) {
    return { error: errorMessage(error) };
  }
  redirect(safeNext(formData.get('next')));
}

export async function register(_: FormState, formData: FormData): Promise<FormState> {
  try {
    await api().auth.registerBuyer(credentials(formData));
  } catch (error) {
    if (error instanceof SdkHttpError && error.status === 409) {
      return { error: 'An account with this email already exists.' };
    }
    return { error: errorMessage(error) };
  }
  redirect(safeNext(formData.get('next')));
}

export async function logout() {
  await api().auth.logout();
  redirect('/');
}

export async function addToCart(_: FormState, formData: FormData): Promise<FormState> {
  const productId = String(formData.get('productId'));
  const user = await getUser();
  if (!user) redirect(`/login?next=${encodeURIComponent(`/products/${productId}`)}`);
  const skuId = Number(formData.get('skuId'));
  if (!skuId) return { error: 'Pick an option first.' };
  try {
    await api().cart.addCartItem({ customerId: user.id, skuId, quantity: Number(formData.get('quantity') || 1) });
  } catch (error) {
    return { error: errorMessage(error) };
  }
  revalidatePath('/', 'layout');
  return { message: 'Added to your cart.' };
}

/** Cart changes report failures through `?error=` on the cart page. */
async function changeCart(change: (customerId: string) => Promise<unknown>) {
  const user = await requireUser('/cart');
  try {
    await change(user.id);
  } catch (error) {
    redirect(`/cart?error=${encodeURIComponent(errorMessage(error))}`);
  }
  revalidatePath('/', 'layout');
}

export async function setQuantity(formData: FormData) {
  const skuId = Number(formData.get('skuId'));
  const quantity = Number(formData.get('quantity'));
  const version = Number(formData.get('version'));
  await changeCart((customerId) =>
    quantity > 0
      ? api().cart.setCartItem({ customerId, skuId, quantity, version })
      : api().cart.removeCartItem({ customerId, skuId, version }),
  );
}

export async function removeItem(formData: FormData) {
  const skuId = Number(formData.get('skuId'));
  const version = Number(formData.get('version'));
  await changeCart((customerId) => api().cart.removeCartItem({ customerId, skuId, version }));
}

export async function startCheckout() {
  await requireUser('/cart');
  let checkoutId: string;
  try {
    ({ checkoutId } = await api().checkout.draftCheckout({}));
  } catch (error) {
    redirect(`/cart?error=${encodeURIComponent(errorMessage(error))}`);
  }
  redirect(`/checkout/${checkoutId}`);
}

export async function placeOrder(formData: FormData) {
  const checkoutId = String(formData.get('checkoutId'));
  await requireUser(`/checkout/${checkoutId}`);
  try {
    await api().checkout.commitCheckout({ checkoutId });
  } catch (error) {
    redirect(`/checkout/${checkoutId}?error=${encodeURIComponent(errorMessage(error))}`);
  }
  revalidatePath('/', 'layout');
  redirect(`/orders/${checkoutId}?placed=1`);
}

export async function cancelCheckout(formData: FormData) {
  const checkoutId = String(formData.get('checkoutId'));
  await requireUser(`/checkout/${checkoutId}`);
  try {
    await api().checkout.cancelCheckout({ checkoutId });
  } catch (error) {
    redirect(`/checkout/${checkoutId}?error=${encodeURIComponent(errorMessage(error))}`);
  }
  redirect('/cart');
}
