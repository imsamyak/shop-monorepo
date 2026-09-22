import { SdkAuthError, SdkHttpError } from '@shop/chatbot-sdk';
import { chatbot } from '@/lib/chatbot';

/** The chat window posts here; the customer's token stays in the httpOnly cookie and goes to the assistant from this server. */
export async function POST(request: Request) {
  const input = (await request.json().catch(() => null)) as { message?: unknown; sessionId?: unknown } | null;
  if (typeof input?.message !== 'string' || typeof input.sessionId !== 'string') {
    return Response.json({ error: 'message and sessionId are required' }, { status: 400 });
  }

  try {
    return Response.json(await chatbot().chat.chat({ message: input.message, sessionId: input.sessionId }));
  } catch (error) {
    if (error instanceof SdkAuthError) return Response.json({ error: 'Sign in to chat' }, { status: 401 });
    if (error instanceof SdkHttpError && error.status !== 500) {
      const message = (error.body as { error?: string } | undefined)?.error;
      return Response.json({ error: message ?? 'The assistant could not answer' }, { status: error.status });
    }
    console.error('chat failed', error);
    return Response.json({ error: 'The assistant is not available right now' }, { status: 502 });
  }
}
