import type { HttpMethod, ServiceName, TokenEffect, UseCase, Validator } from './types.js';

interface Draft {
  readonly name: string;
  readonly service?: ServiceName;
  readonly method?: HttpMethod;
  readonly path?: string;
  readonly validators: UseCase['validators'];
  readonly authRequired: boolean;
  readonly tokenEffect?: TokenEffect;
  readonly multipart?: boolean;
  readonly mapResponse?: (response: unknown) => unknown;
}

/**
 * Declares a use case step by step, tracking the types each step adds so `build()` returns a fully typed `UseCase`.
 * Every step returns a new builder.
 *
 * ```ts
 * export const removeCartItem = useCase('RemoveCartItem')
 *   .service('checkout')
 *   .delete('/carts/{customerId}/items/{skuId}')
 *   .params(cartItemParams)
 *   .query(cartVersionQuery)
 *   .returns<CartResponse>()
 *   .build();
 * ```
 */
export class UseCaseBuilder<N extends string, P = undefined, Q = undefined, B = undefined, R = void> {
  private constructor(private readonly draft: Draft) {}

  static named<const N extends string>(name: N): UseCaseBuilder<N> {
    return new UseCaseBuilder({ name, validators: {}, authRequired: false });
  }

  service(service: ServiceName): UseCaseBuilder<N, P, Q, B, R> {
    return this.with({ service });
  }

  get(path: string): UseCaseBuilder<N, P, Q, B, R> {
    return this.with({ method: 'GET', path });
  }

  post(path: string): UseCaseBuilder<N, P, Q, B, R> {
    return this.with({ method: 'POST', path });
  }

  put(path: string): UseCaseBuilder<N, P, Q, B, R> {
    return this.with({ method: 'PUT', path });
  }

  patch(path: string): UseCaseBuilder<N, P, Q, B, R> {
    return this.with({ method: 'PATCH', path });
  }

  delete(path: string): UseCaseBuilder<N, P, Q, B, R> {
    return this.with({ method: 'DELETE', path });
  }

  /** Validates the values of the path's `{name}` placeholders. */
  params<T>(validator: Validator<T>): UseCaseBuilder<N, T, Q, B, R> {
    return this.with({ validators: { ...this.draft.validators, params: validator } });
  }

  query<T>(validator: Validator<T>): UseCaseBuilder<N, P, T, B, R> {
    return this.with({ validators: { ...this.draft.validators, query: validator } });
  }

  body<T>(validator: Validator<T>): UseCaseBuilder<N, P, Q, T, R> {
    return this.with({ validators: { ...this.draft.validators, body: validator } });
  }

  /** Sends the stored access token; without one the call fails before reaching the backend. */
  authRequired(required = true): UseCaseBuilder<N, P, Q, B, R> {
    return this.with({ authRequired: required });
  }

  /** Stores the `accessToken` of the response. */
  storesToken(): UseCaseBuilder<N, P, Q, B, R> {
    return this.with({ tokenEffect: 'set' });
  }

  /** Clears the stored token once the call succeeds. */
  clearsToken(): UseCaseBuilder<N, P, Q, B, R> {
    return this.with({ tokenEffect: 'clear' });
  }

  /** Sends the body as `multipart/form-data`: a `Blob` field is a file, an array of them is several files in order. */
  multipart(): UseCaseBuilder<N, P, Q, B, R> {
    return this.with({ multipart: true });
  }

  /** The response body type; leave it out for an empty response. */
  returns<T>(): UseCaseBuilder<N, P, Q, B, T> {
    return this.with({});
  }

  /** Reshapes the response body before it is returned; call after `returns`, which types its input. */
  mapResponse<T>(map: (response: R) => T): UseCaseBuilder<N, P, Q, B, T> {
    return this.with({ mapResponse: map as (response: unknown) => unknown });
  }

  build(): UseCase<N, P, Q, B, R> {
    const { name, service, method, path, validators, authRequired, tokenEffect, multipart, mapResponse } = this.draft;
    if (!service) throw new Error(`Use case '${name}' has no service`);
    if (!method || !path) throw new Error(`Use case '${name}' has no method and path`);
    return Object.freeze({
      name: name as N,
      service,
      method,
      path,
      validators: Object.freeze({ ...validators }) as UseCase<N, P, Q, B, R>['validators'],
      authRequired,
      ...(tokenEffect && { tokenEffect }),
      ...(multipart && { multipart }),
      ...(mapResponse && { mapResponse }),
    });
  }

  private with<P2 = P, Q2 = Q, B2 = B, R2 = R>(change: Partial<Draft>): UseCaseBuilder<N, P2, Q2, B2, R2> {
    return new UseCaseBuilder({ ...this.draft, ...change });
  }
}

/** Starts a use case declaration. */
export const useCase = <const N extends string>(name: N): UseCaseBuilder<N> => UseCaseBuilder.named(name);
