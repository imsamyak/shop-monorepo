import { describe, expect, it } from 'vitest';
import { allUseCases, resolveBaseUrls } from '../src/index.js';

describe('resolveBaseUrls', () => {
  it('falls back to the local service ports', () => {
    expect(resolveBaseUrls({})).toMatchObject({ identity: 'http://localhost:8086', inventory: 'http://localhost:8083', asset: 'http://localhost:8088' });
  });

  it('prefers the per-service variable over ECOM_API_URL, and options over both', () => {
    const urls = resolveBaseUrls(
      { ECOM_API_URL: 'https://api.example.com/', ECOM_ORDER_URL: 'https://orders.example.com' },
      { identity: 'http://override' },
    );
    expect(urls).toMatchObject({
      catalog: 'https://api.example.com',
      order: 'https://orders.example.com',
      identity: 'http://override',
    });
  });
});

describe('services', () => {
  it('gives every service a use case names a base URL', () => {
    const urls = resolveBaseUrls({});
    for (const useCase of allUseCases) expect(urls, useCase.name).toHaveProperty(useCase.service);
  });
});
