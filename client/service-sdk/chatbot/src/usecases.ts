import { useCase } from '@shop/sdk-core';
import { z } from 'zod';

/** Same limits as `server/chatbot`: 2000 characters of message, a session id of at most 100. */
export const chatBody = z.strictObject({
  message: z.string().regex(/\S/, 'must not be blank').max(2000),
  sessionId: z.string().min(1).max(100),
});

export interface ChatResponse {
  reply: string;
}

/** The customer is whoever the access token says; the answer remembers earlier messages with the same `sessionId`. */
export const chat = useCase('Chat')
  .service('chatbot')
  .post('/api/chat')
  .body(chatBody)
  .returns<ChatResponse>()
  .authRequired()
  .build();

export const chatbotUseCases = [chat] as const;
export const allUseCases = chatbotUseCases;
