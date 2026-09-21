import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it, vi } from 'vitest';
import { allUseCases, createChatbotSdk, resolveBaseUrls } from '../src/index.js';

/** Keeps the SDK in step with the service: `server/chatbot/docs/api.json` lists the endpoints. */
interface Endpoint {
  usecase: string;
  api: string;
  auth: { required: boolean };
}
const api = JSON.parse(readFileSync(join(import.meta.dirname, '../../../../server/chatbot/docs/api.json'), 'utf8')) as {
  services: Record<string, { endpoints: Endpoint[] }>;
};
const endpoints = Object.values(api.services).flatMap((service) => service.endpoints);

describe('server/chatbot/docs/api.json', () => {
  it.each(endpoints.map((e) => [e.usecase, e] as const))('%s is a use case of the SDK', (_, endpoint) => {
    const useCase = allUseCases.find((u) => u.name === endpoint.usecase);
    expect(useCase, `no use case named ${endpoint.usecase}`).toBeDefined();
    expect(`${useCase?.method} ${useCase?.path}`).toBe(endpoint.api);
    expect(useCase?.authRequired).toBe(endpoint.auth.required);
  });

  it('has no use case missing from the list', () => {
    expect(allUseCases.map((u) => u.name).sort()).toEqual(endpoints.map((e) => e.usecase).sort());
  });
});

describe('chat', () => {
  const answer = (status: number, body: unknown) =>
    vi.fn(async () => new Response(JSON.stringify(body), { status, headers: { 'content-type': 'application/json' } }));
  const withToken = (token: string | undefined) => () => ({ getAccessToken: () => token, setAccessToken: () => {} });

  it('posts the message with the customer token', async () => {
    const fetch = answer(200, { reply: 'hello' });
    const sdk = createChatbotSdk({ tokenStore: withToken('customer-token'), baseUrls: { chatbot: 'http://bot' }, fetch });
    await expect(sdk.with().chat.chat({ message: 'hi', sessionId: 's1' })).resolves.toEqual({ reply: 'hello' });
    const [url, init] = fetch.mock.calls[0] as unknown as [string, RequestInit];
    expect(url).toBe('http://bot/api/chat');
    expect(init.method).toBe('POST');
    expect((init.headers as Record<string, string>).authorization).toBe('Bearer customer-token');
    expect(JSON.parse(init.body as string)).toEqual({ message: 'hi', sessionId: 's1' });
  });

  it('refuses a blank or (when validating strictly) overlong message before sending', async () => {
    const fetch = answer(200, { reply: '' });
    const sdk = createChatbotSdk({ tokenStore: withToken('t'), fetch, validation: 'strict' });
    await expect(sdk.with().chat.chat({ message: '  ', sessionId: 's' })).rejects.toThrow();
    await expect(sdk.with().chat.chat({ message: 'x'.repeat(2001), sessionId: 's' })).rejects.toThrow();
    expect(fetch).not.toHaveBeenCalled();
  });

  it('needs a token', async () => {
    const fetch = answer(200, { reply: '' });
    const sdk = createChatbotSdk({ tokenStore: withToken(undefined), fetch });
    await expect(sdk.with().chat.chat({ message: 'hi', sessionId: 's' })).rejects.toThrow();
    expect(fetch).not.toHaveBeenCalled();
  });
});

describe('resolveBaseUrls', () => {
  it('uses the local port, CHATBOT_URL, then an override', () => {
    expect(resolveBaseUrls({})).toEqual({ chatbot: 'http://localhost:4001' });
    expect(resolveBaseUrls({ CHATBOT_URL: 'https://bot.example.com/' })).toEqual({ chatbot: 'https://bot.example.com' });
    expect(resolveBaseUrls({ CHATBOT_URL: 'https://bot.example.com' }, { chatbot: 'http://x' })).toEqual({ chatbot: 'http://x' });
  });
});
