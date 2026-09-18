import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import { allUseCases } from '../src/index.js';

/**
 * Keeps the SDK in step with the backend: `server/platform/docs/api.json` lists every endpoint the frontend may call, and each
 * must be a use case here with the same name, method, path and sign-in requirement.
 */
interface Endpoint {
  usecase: string;
  api: string;
  auth: { required: boolean };
}

const api = JSON.parse(readFileSync(join(import.meta.dirname, '../../../../server/platform/docs/api.json'), 'utf8')) as {
  services: Record<string, { endpoints: Endpoint[] }>;
};
const endpoints = Object.entries(api.services).flatMap(([service, { endpoints }]) => endpoints.map((e) => ({ service, ...e })));

describe('server/platform/docs/api.json', () => {
  it.each(endpoints.map((e) => [e.usecase, e] as const))('%s is a use case of the SDK', (_, endpoint) => {
    const useCase = allUseCases.find((u) => u.name === endpoint.usecase);
    expect(useCase, `no use case named ${endpoint.usecase}`).toBeDefined();
    expect(`${useCase?.method} ${useCase?.path}`).toBe(endpoint.api);
    expect(useCase?.authRequired).toBe(endpoint.auth.required);
  });

  it('is not missing an endpoint that the SDK has for the public API', () => {
    const internal = new Set(['GetReservation', 'ListReservations', 'GetInventories']);
    const known = new Set(endpoints.map((e) => e.usecase));
    const extra = allUseCases.map((u) => u.name).filter((name) => !known.has(name) && !internal.has(name));
    expect(extra).toEqual([]);
  });
});
