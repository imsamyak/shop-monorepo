'use server';

import { SdkHttpError } from '@shop/platform-sdk';
import { revalidatePath } from 'next/cache';
import { redirect } from 'next/navigation';
import { errorMessage } from './errors';
import { api } from './sdk';
import { requireUser } from './session';

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
