import { vi } from 'vitest';
import type { TokenStore } from '../src/index.js';

export interface RecordedRequest {
  url: string;
  method: string;
  headers: Record<string, string>;
  body: unknown;
}

/** A `fetch` that answers every call with `status` and `body`, and records what it was sent. */
export function fakeFetch(status = 200, body?: unknown) {
  const requests: RecordedRequest[] = [];
  const fetch = vi.fn(async (url: string | URL | Request, init?: RequestInit) => {
    requests.push({
      url: String(url),
      method: init?.method ?? 'GET',
      headers: (init?.headers ?? {}) as Record<string, string>,
      body: typeof init?.body === 'string' ? JSON.parse(init.body) : undefined,
    });
    const text = body === undefined ? null : JSON.stringify(body);
    return new Response(text, { status });
  });
  return { fetch: fetch as unknown as typeof globalThis.fetch, requests };
}

/** One token per context object, the way a server keeps one per user's cookie. */
export interface Ctx {
  token?: string;
}

export const ctxTokenStore = (ctx: Ctx): TokenStore => ({
  getAccessToken: () => ctx.token,
  setAccessToken: (token) => {
    ctx.token = token;
  },
});

export const CUSTOMER = '3f2b8c1e-4a5d-4e6f-8a9b-0c1d2e3f4a5b';
