export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

/** The backend service a use case is sent to; each one has its own base URL (`SdkOptions.baseUrls`). */
export type ServiceName = string;

/** The part of a request a validator checks. */
export type RequestPart = 'params' | 'query' | 'body';

export interface ValidationIssue {
  readonly code: string;
  readonly path: readonly PropertyKey[];
  readonly message: string;
  /** Present on `too_small` issues. */
  readonly minimum?: number | bigint;
}

export type ValidationOutcome<T> =
  | { readonly success: true; readonly data: T }
  | { readonly success: false; readonly error: { readonly issues: readonly ValidationIssue[] } };

/**
 * Checks one part of a request. Shaped like zod's `safeParse`, so any zod schema is a validator as it is, but nothing
 * here depends on zod.
 */
export interface Validator<T> {
  safeParse(input: unknown): ValidationOutcome<T>;
}

/** `set` stores the `accessToken` of the response, `clear` removes the stored token. */
export type TokenEffect = 'set' | 'clear';

/**
 * One backend endpoint. `P`, `Q` and `B` are the fields of its params, query and body (`undefined` when it has none);
 * the caller passes them merged into one request. `R` is the response body.
 */
export interface UseCase<N extends string = string, P = unknown, Q = unknown, B = unknown, R = unknown> {
  readonly name: N;
  readonly service: ServiceName;
  readonly method: HttpMethod;
  /** With `{name}` placeholders for the path params, e.g. `/carts/{customerId}/items/{skuId}`. */
  readonly path: string;
  readonly validators: {
    readonly params?: Validator<P>;
    readonly query?: Validator<Q>;
    readonly body?: Validator<B>;
  };
  /** Sends `Authorization: Bearer <token>`; the call fails before reaching the backend when there is no token. */
  readonly authRequired: boolean;
  readonly tokenEffect?: TokenEffect;
  /** Sends the body as `multipart/form-data` (files) instead of JSON. */
  readonly multipart?: boolean;
  /** Reshapes a successful response body before it is returned. */
  readonly mapResponse?: (response: unknown) => unknown;
  /** Type-only marker for the response body; never read at runtime. */
  readonly response?: R;
}

export type AnyUseCase = UseCase<string, any, any, any, any>;

type Fields<T> = [T] extends [undefined] ? {} : T;

type Simplify<T> = { [K in keyof T]: T[K] } & {};

/**
 * What a caller passes for a use case: the fields of its params, query and body in one object. The SDK sorts them back
 * into their parts (see `splitRequest`).
 */
export type UseCaseRequest<U> =
  U extends UseCase<string, infer P, infer Q, infer B, unknown> ? Simplify<Fields<P> & Fields<Q> & Fields<B>> : never;

/** The request argument: optional when the use case has no required field. */
export type RequestArgs<U> = {} extends UseCaseRequest<U> ? [request?: UseCaseRequest<U>] : [request: UseCaseRequest<U>];

export type UseCaseResult<U> = U extends UseCase<string, unknown, unknown, unknown, infer R> ? R : never;

/** Use cases keyed by name, as the builder collects them. */
export type UseCaseMap = Record<string, AnyUseCase>;
