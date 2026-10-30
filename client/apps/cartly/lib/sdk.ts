import 'server-only';
import { createEcomSdk } from '@shop/platform-sdk';
import { cookies } from 'next/headers';

export const TOKEN_COOKIE = 'access_token';

/** The token lives in an httpOnly cookie; it can only be written from server actions and route handlers. */
export const sdk = createEcomSdk({
  tokenStore: () => ({
    getAccessToken: async () => (await cookies()).get(TOKEN_COOKIE)?.value,
    setAccessToken: async (token) => {
      const jar = await cookies();
      if (token) {
        jar.set(TOKEN_COOKIE, token, {
          httpOnly: true,
          sameSite: 'lax',
          path: '/',
          // browsers drop a Secure cookie that arrives over plain http; COOKIE_SECURE=false is for a deployment without https
          secure: (process.env.COOKIE_SECURE ?? String(process.env.NODE_ENV === 'production')) === 'true',
        });
      } else {
        jar.delete(TOKEN_COOKIE);
      }
    },
  }),
});

export const api = () => sdk.with();
