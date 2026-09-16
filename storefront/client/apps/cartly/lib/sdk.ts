import 'server-only';
import { createEcomSdk } from '@shop/platform-sdk';
import { cookies } from 'next/headers';

const TOKEN_COOKIE = 'access_token';

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
          secure: process.env.NODE_ENV === 'production',
        });
      } else {
        jar.delete(TOKEN_COOKIE);
      }
    },
  }),
});

export const api = () => sdk.with();
