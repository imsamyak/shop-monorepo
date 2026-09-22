import 'server-only';
import { createChatbotSdk } from '@shop/chatbot-sdk';
import { cookies } from 'next/headers';
import { TOKEN_COOKIE } from './sdk';

/** The assistant is asked as the signed-in customer: their cookie token is what it passes on to the platform. */
const sdk = createChatbotSdk({
  tokenStore: () => ({
    getAccessToken: async () => (await cookies()).get(TOKEN_COOKIE)?.value,
    setAccessToken: () => {},
  }),
});

export const chatbot = () => sdk.with();
