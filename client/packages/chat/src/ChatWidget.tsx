'use client';

import { type FormEvent, type ReactNode, useEffect, useRef, useState } from 'react';

export interface ChatWidgetProps {
  /** Where messages are posted as `{ message, sessionId }`; answers `{ reply }`. The app's server adds the customer's token. */
  endpoint?: string;
  /** Name shown in the window's title. */
  title?: string;
}

interface Message {
  from: 'you' | 'bot';
  text: string;
}

const SESSION_KEY = 'chat-session';

/** `**bold**` inside one line of a reply; everything else stays plain text, so nothing a seller wrote becomes markup. */
function inline(line: string): ReactNode[] {
  return line.split(/(\*\*[^*]+\*\*)/g).map((part, i) =>
    part.startsWith('**') && part.endsWith('**') && part.length > 4 ? <strong key={i}>{part.slice(2, -2)}</strong> : part,
  );
}

/** The assistant answers in light markdown: paragraphs, `- ` or `* ` bullets and bold. */
function Reply({ text }: { text: string }) {
  const blocks: ReactNode[] = [];
  let bullets: string[] = [];
  const flush = () => {
    if (bullets.length) {
      blocks.push(
        <ul key={`ul${blocks.length}`}>
          {bullets.map((item, i) => (
            <li key={i}>{inline(item)}</li>
          ))}
        </ul>,
      );
      bullets = [];
    }
  };
  for (const raw of text.split('\n')) {
    const bullet = /^\s*[-*]\s+(.*)$/.exec(raw);
    if (bullet) {
      bullets.push(bullet[1] ?? '');
      continue;
    }
    flush();
    if (/^\s*-{3,}\s*$/.test(raw)) {
      blocks.push(<hr key={`hr${blocks.length}`} />);
      continue;
    }
    const heading = /^\s*#{1,6}\s+(.*)$/.exec(raw);
    if (heading) blocks.push(<p key={`h${blocks.length}`}><strong>{inline(heading[1] ?? '')}</strong></p>);
    else if (raw.trim()) blocks.push(<p key={`p${blocks.length}`}>{inline(raw)}</p>);
  }
  flush();
  return <>{blocks}</>;
}

/** One conversation per browser tab: the id survives a reload so the assistant still remembers the thread. */
function sessionId(): string {
  try {
    const saved = sessionStorage.getItem(SESSION_KEY);
    if (saved) return saved;
    const created = crypto.randomUUID();
    sessionStorage.setItem(SESSION_KEY, created);
    return created;
  } catch {
    return crypto.randomUUID();
  }
}

async function ask(endpoint: string, message: string, id: string): Promise<string> {
  let response: Response;
  try {
    response = await fetch(endpoint, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ message, sessionId: id }),
    });
  } catch {
    return 'I could not reach the assistant. Check your connection and try again.';
  }
  const body = (await response.json().catch(() => ({}))) as { reply?: string; error?: string };
  if (response.ok && body.reply) return body.reply;
  if (response.status === 401) return 'Please sign in again to keep chatting.';
  return body.error ?? 'Something went wrong. Please try again.';
}

export function ChatWidget({ endpoint = '/api/chat', title = 'Shopping assistant' }: ChatWidgetProps) {
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [text, setText] = useState('');
  const [messages, setMessages] = useState<Message[]>([
    { from: 'bot', text: 'Hi! Ask me about products, your cart, your orders, shipping or returns.' },
  ]);
  const id = useRef<string | null>(null);
  const end = useRef<HTMLDivElement>(null);

  useEffect(() => {
    end.current?.scrollIntoView({ block: 'end' });
  }, [messages, open]);

  async function send(event: FormEvent) {
    event.preventDefault();
    const message = text.trim();
    if (!message || busy) return;
    id.current ??= sessionId();
    setText('');
    setBusy(true);
    setMessages((all) => [...all, { from: 'you', text: message }]);
    const reply = await ask(endpoint, message, id.current);
    setMessages((all) => [...all, { from: 'bot', text: reply }]);
    setBusy(false);
  }

  return (
    <div className="cc-root">
      <style>{CSS}</style>
      {open ? (
        <section className="cc-window" aria-label={title}>
          <header className="cc-head">
            <strong>{title}</strong>
            <button type="button" className="cc-close" onClick={() => setOpen(false)} aria-label="Close chat">
              ×
            </button>
          </header>
          <div className="cc-log" role="log" aria-live="polite">
            {messages.map((m, i) => (
              <div key={i} className={`cc-msg cc-${m.from}`}>
                {m.from === 'bot' ? <Reply text={m.text} /> : m.text}
              </div>
            ))}
            {busy && <div className="cc-msg cc-bot cc-wait">Thinking…</div>}
            <div ref={end} />
          </div>
          <form className="cc-form" onSubmit={send}>
            <input
              value={text}
              onChange={(e) => setText(e.target.value)}
              placeholder="Type a message"
              aria-label="Message"
              maxLength={2000}
              autoComplete="off"
            />
            <button type="submit" disabled={busy || !text.trim()}>
              Send
            </button>
          </form>
        </section>
      ) : (
        <button type="button" className="cc-open" onClick={() => setOpen(true)}>
          Chat with us
        </button>
      )}
    </div>
  );
}

// Colours come from the app's own variables (Cartly defines them), with fallbacks for apps that do not.
const CSS = `
.cc-root { position: fixed; right: 16px; bottom: 16px; z-index: 50; font: inherit; }
.cc-open { background: var(--accent, #1f6f5c); color: var(--accent-text, #fff); border: 0; border-radius: 999px; padding: 12px 18px; cursor: pointer; font: inherit; box-shadow: 0 4px 14px rgba(0,0,0,.2); }
.cc-window { width: min(360px, calc(100vw - 32px)); height: min(520px, calc(100vh - 32px)); display: flex; flex-direction: column; background: var(--surface, #fff); color: var(--text, #1c1c1a); border: 1px solid var(--border, #e4e4df); border-radius: var(--radius, 10px); box-shadow: 0 8px 30px rgba(0,0,0,.25); overflow: hidden; }
.cc-head { display: flex; justify-content: space-between; align-items: center; padding: 10px 14px; border-bottom: 1px solid var(--border, #e4e4df); }
.cc-close { background: none; border: 0; color: inherit; font-size: 22px; line-height: 1; cursor: pointer; }
.cc-log { flex: 1; overflow-y: auto; padding: 12px; display: flex; flex-direction: column; gap: 8px; }
.cc-msg p { margin: 0 0 6px; }
.cc-msg p:last-child, .cc-msg ul:last-child { margin-bottom: 0; }
.cc-msg hr { border: 0; border-top: 1px solid var(--border, #e4e4df); margin: 8px 0; }
.cc-msg ul { margin: 0 0 6px; padding-left: 18px; }
.cc-msg { margin: 0; padding: 8px 12px; border-radius: 12px; max-width: 85%; white-space: pre-wrap; overflow-wrap: anywhere; }
.cc-bot { background: var(--bg, #f7f7f5); align-self: flex-start; }
.cc-you { background: var(--accent, #1f6f5c); color: var(--accent-text, #fff); align-self: flex-end; }
.cc-wait { color: var(--muted, #6b6b66); }
.cc-form { display: flex; gap: 8px; padding: 10px; border-top: 1px solid var(--border, #e4e4df); }
.cc-form input { flex: 1; min-width: 0; padding: 8px 10px; border: 1px solid var(--border, #e4e4df); border-radius: 8px; background: var(--bg, #f7f7f5); color: inherit; font: inherit; }
.cc-form button { background: var(--accent, #1f6f5c); color: var(--accent-text, #fff); border: 0; border-radius: 8px; padding: 8px 14px; cursor: pointer; font: inherit; }
.cc-form button:disabled { opacity: .5; cursor: default; }
`;
