import 'server-only';
import { SdkAuthError, SdkHttpError, type UserResponse } from '@shop/platform-sdk';
import { redirect } from 'next/navigation';
import { cache } from 'react';
import { api } from './sdk';

/** The signed-in user, or null when there is no token or it was rejected. */
export const getUser = cache(async (): Promise<UserResponse | null> => {
  try {
    return await api().auth.getCurrentUser();
  } catch (error) {
    if (error instanceof SdkAuthError) return null;
    if (error instanceof SdkHttpError && (error.status === 401 || error.status === 403)) return null;
    throw error;
  }
});

/** The signed-in user; anyone else is sent to the login page and back to `from` afterwards. */
export async function requireUser(from: string): Promise<UserResponse> {
  const user = await getUser();
  if (!user) redirect(`/login?next=${encodeURIComponent(from)}`);
  return user;
}
