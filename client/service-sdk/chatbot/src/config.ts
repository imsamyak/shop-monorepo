/** Address of the chatbot: `CHATBOT_URL`, else the local port. */
export const CHATBOT_URL_ENV = 'CHATBOT_URL';
const LOCAL_DEFAULT = 'http://localhost:4001';

type Env = Record<string, string | undefined>;

export type BaseUrls = { readonly chatbot: string };

function processEnv(): Env {
  return (globalThis as { process?: { env?: Env } }).process?.env ?? {};
}

export function resolveBaseUrls(env: Env = processEnv(), overrides: Partial<BaseUrls> = {}): BaseUrls {
  return { chatbot: (overrides.chatbot ?? env[CHATBOT_URL_ENV] ?? LOCAL_DEFAULT).replace(/\/+$/, '') };
}
