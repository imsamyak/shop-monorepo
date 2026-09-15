/** Where the SDK reads and writes the caller's access token. */
export interface TokenStore {
  getAccessToken(): string | undefined | Promise<string | undefined>;
  /** `undefined` clears the token (logout). */
  setAccessToken(token: string | undefined): void | Promise<void>;
}

/**
 * Supplied by the client and called on every `execute` with that call's context (for example the incoming request and
 * response on a server, so the token can live in that user's cookie).
 */
export type TokenStoreProvider<Ctx> = (ctx: Ctx) => TokenStore;
