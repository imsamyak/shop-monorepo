# @shop/chatbot-sdk

Typed client of `server/chatbot`, built on `@shop/sdk-core`. Its endpoint list is `server/chatbot/docs/api.json`, and
`tests/chatbot-sdk.test.ts` fails when the two drift apart.

```ts
const sdk = createChatbotSdk({ tokenStore: (token: string) => ({ getAccessToken: () => token, setAccessToken: () => {} }) });
const { reply } = await sdk.with(accessToken).chat.chat({ message: 'Do you ship to Pune?', sessionId });
```

The address is `CHATBOT_URL` (default `http://localhost:4001`).
