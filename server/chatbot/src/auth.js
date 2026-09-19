// WHO IS CHATTING: the caller's access token, taken from the Authorization header.
//
// The chatbot never decides what a customer may see. It passes the caller's own token on to the platform services,
// and they verify it and enforce ownership. Reading `sub` out of the token here only tells us which customer id to put
// in a URL such as /carts/{customerId}; a forged token gets nowhere because the services reject it.

export function bearerToken(req) {
  const header = req.get("authorization") || "";
  const match = /^Bearer\s+(\S+)$/i.exec(header);
  return match ? match[1] : null;
}

export function userIdOf(token) {
  try {
    const payload = JSON.parse(Buffer.from(token.split(".")[1], "base64url").toString("utf8"));
    return typeof payload.sub === "string" && payload.sub ? payload.sub : null;
  } catch {
    return null;
  }
}
