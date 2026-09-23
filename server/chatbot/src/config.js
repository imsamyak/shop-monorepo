// All settings live here, read from environment variables (the .env file).
// Keeping them in one place means no other file needs to know about process.env.
//
// The platform's own addresses are not here: @shop/platform-sdk reads ECOM_API_URL / ECOM_<SERVICE>_URL itself.

export const config = {
  port: Number(process.env.PORT || 4001),

  // Gemini API key and model
  geminiApiKey: process.env.GEMINI_API_KEY || process.env.CHATBOT_API_KEY_GEMINI,
  model: process.env.MODEL || "gemini-3.8-flash",

  // The little test page in public/ is for development only
  servePublicPage: process.env.NODE_ENV !== "production",

  // Limits on what one request and one idle conversation may cost
  maxMessageChars: 2000,
  sessionIdleMs: 60 * 60 * 1000,
  maxSessions: 1000,
};
