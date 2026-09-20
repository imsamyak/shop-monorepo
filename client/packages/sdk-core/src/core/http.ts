import type { HttpMethod, RequestPart } from './types.js';

const PLACEHOLDER = /\{(\w+)\}/g;

export type RequestParts = Record<RequestPart, Record<string, unknown>>;

/**
 * Sorts a flat request into its parts: fields named by a path placeholder go to `params`, every other field to `query`
 * on GET and DELETE and to `body` otherwise.
 */
export function splitRequest(method: HttpMethod, path: string, request: object | undefined): RequestParts {
  const pathKeys = new Set(placeholders(path));
  const params: Record<string, unknown> = {};
  const rest: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(request ?? {})) {
    (pathKeys.has(key) ? params : rest)[key] = value;
  }
  const restInQuery = method === 'GET' || method === 'DELETE';
  return { params, query: restInQuery ? rest : {}, body: restInQuery ? {} : rest };
}

/** The `{name}` placeholders of a path template, in order. */
export function placeholders(path: string): string[] {
  return Array.from(path.matchAll(PLACEHOLDER), (match) => match[1] as string);
}

export function fillPath(path: string, params: Record<string, unknown> | undefined): string {
  return path.replace(PLACEHOLDER, (_, name: string) => {
    const value = params?.[name];
    if (value === undefined || value === null) {
      throw new Error(`Missing path param '${name}' for ${path}`);
    }
    return encodeURIComponent(String(value));
  });
}

/** `?a=1&ids=1&ids=2`, skipping undefined and null; empty when nothing is left. */
export function toQueryString(query: Record<string, unknown> | undefined): string {
  if (!query) return '';
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value === undefined || value === null) continue;
    for (const item of Array.isArray(value) ? value : [value]) {
      search.append(key, String(item));
    }
  }
  const text = search.toString();
  return text ? `?${text}` : '';
}
