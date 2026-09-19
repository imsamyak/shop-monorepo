// THE WEB SERVER (Express). Entry point: `pnpm start`.
//
// POST /api/chat
//   header: Authorization: Bearer <the customer's access token>
//   body:   { message, sessionId }
//   returns: { reply }
//
// The customer is whoever the token says; it is never taken from the body.

import { fileURLToPath } from "node:url";
import express from "express";
import { config } from "./config.js";
import { bearerToken, userIdOf } from "./auth.js";
import { loadIndex } from "./rag/vectorStore.js";
import { newSession, chat } from "./agent/agent.js";

const app = express();
app.use(express.json({ limit: "16kb" }));
if (config.servePublicPage) app.use(express.static(fileURLToPath(new URL("../public", import.meta.url))));

// MEMORY: each chat session keeps its own conversation, so the bot
// remembers what was said earlier ("add 2 of THAT one").
// Stored in memory: restarting the server forgets everything. Use Redis later.
// The key holds the customer too, so one customer cannot continue another's conversation by guessing its id.
const sessions = new Map(); // `${userId}:${sessionId}` -> session

function sessionFor(userId, sessionId) {
  const now = Date.now();
  for (const [key, s] of sessions) if (now - s.lastUsed > config.sessionIdleMs) sessions.delete(key);

  const key = `${userId}:${sessionId}`;
  let session = sessions.get(key);
  if (!session) {
    if (sessions.size >= config.maxSessions) return null;
    session = newSession();
    sessions.set(key, session);
  }
  session.lastUsed = now;
  return session;
}

app.post("/api/chat", async (req, res) => {
  const token = bearerToken(req);
  const userId = token && userIdOf(token);
  if (!userId) return res.status(401).json({ error: "Sign in to chat" });

  const { message, sessionId } = req.body ?? {};
  if (typeof message !== "string" || !message.trim() || typeof sessionId !== "string" || !sessionId || sessionId.length > 100) {
    return res.status(400).json({ error: "message and sessionId are required" });
  }
  if (message.length > config.maxMessageChars) {
    return res.status(400).json({ error: `message must be at most ${config.maxMessageChars} characters` });
  }

  const session = sessionFor(userId, sessionId);
  if (!session) return res.status(503).json({ error: "The assistant is busy, try again later" });

  const ctx = { token, userId };
  try {
    const reply = await chat(session, message, ctx);
    if (ctx.authFailed) return res.status(401).json({ error: "Your session has expired, sign in again" });
    res.json({ reply });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "The assistant could not answer right now" });
  }
});

app.get("/health", (_req, res) => res.json({ status: "ok" }));

loadIndex();
app.listen(config.port, () => console.log(`Chatbot running on http://localhost:${config.port}`));
