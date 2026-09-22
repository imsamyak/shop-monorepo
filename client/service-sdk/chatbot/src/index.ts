import { SdkBuilder, type SdkOptions } from '@shop/sdk-core';
import { type BaseUrls, resolveBaseUrls } from './config.js';
import { chatbotUseCases } from './usecases.js';

export { type BaseUrls, CHATBOT_URL_ENV, resolveBaseUrls } from './config.js';
export * from './usecases.js';
// the engine, so an app needs only this package
export * from '@shop/sdk-core';

/** The engine's options; `baseUrls` only overrides the address read from the environment. */
export type ChatbotSdkOptions<Ctx> = Omit<SdkOptions<Ctx>, 'baseUrls'> & { readonly baseUrls?: Partial<BaseUrls> };

/** `createChatbotSdk(options).with(ctx).chat.chat({ message, sessionId })` */
export function createChatbotSdk<Ctx = undefined>(options: ChatbotSdkOptions<Ctx>) {
  return SdkBuilder.create<Ctx>({ ...options, baseUrls: resolveBaseUrls(undefined, options.baseUrls) })
    .addGateway('chat', chatbotUseCases)
    .build();
}

export type ChatbotSdk<Ctx = undefined> = ReturnType<typeof createChatbotSdk<Ctx>>;
