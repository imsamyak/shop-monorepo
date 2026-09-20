import { readdirSync } from 'node:fs';
import { join } from 'node:path';
import { pathToFileURL } from 'node:url';
import { describe, expect, it } from 'vitest';
import type { z } from 'zod';
import { placeholders } from '@shop/sdk-core';
import type { AnyUseCase } from '../src/index.js';
import { allUseCases } from '../src/index.js';

/** Guards the rules that keep adding and removing use cases safe. */

const USECASES_DIR = join(import.meta.dirname, '../src/usecases');

const isUseCase = (value: unknown): boolean =>
  typeof value === 'object' && value !== null && 'path' in value && 'method' in value && 'service' in value;

const keysOf = (validator: unknown): string[] =>
  validator ? Object.keys((validator as z.ZodObject).shape).sort() : [];

describe.each(allUseCases.map((u) => [u.name, u] as [string, AnyUseCase]))('%s', (_, useCase) => {
  it('has a params validator for exactly the path placeholders', () => {
    expect(keysOf(useCase.validators.params)).toEqual(placeholders(useCase.path).sort());
  });

  // The flat request is split by these two rules (see splitRequest), so a use case must not break them.
  it('takes no body on GET and DELETE, and no query otherwise', () => {
    const readOnly = useCase.method === 'GET' || useCase.method === 'DELETE';
    expect(readOnly ? useCase.validators.body : useCase.validators.query).toBeUndefined();
  });

  it('has no field name in two parts', () => {
    const { params, query, body } = useCase.validators;
    const all = [...keysOf(params), ...keysOf(query), ...keysOf(body)];
    expect(all.length).toBe(new Set(all).size);
  });
});

describe('registry', () => {
  it('has unique names', () => {
    const names = allUseCases.map((u) => u.name);
    expect(new Set(names).size).toBe(names.length);
  });

  it("lists every exported use case in its service's list", async () => {
    const listed = new Set<unknown>(allUseCases);
    const services = readdirSync(USECASES_DIR, { withFileTypes: true }).filter((d) => d.isDirectory() && d.name !== 'shared');
    const missing: string[] = [];
    for (const service of services) {
      const url = pathToFileURL(join(USECASES_DIR, service.name, 'index.ts')).href;
      const module = (await import(url)) as Record<string, unknown>;
      for (const [exported, value] of Object.entries(module)) {
        if (isUseCase(value) && !listed.has(value)) missing.push(`${service.name}: ${exported}`);
      }
    }
    expect(missing).toEqual([]);
  });
});
