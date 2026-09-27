'use client';

import { AlertIcon } from '@/components/icons';
import { ErrorScreen } from '@/components/ErrorScreen';
import './globals.css';

export default function GlobalError({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return (
    <html lang="en">
      <body>
        <main className="page">
          <ErrorScreen
            icon={<AlertIcon />}
            eyebrow="Something went wrong"
            title="Sorry, something went wrong on our end."
            message="Please try again in a moment."
            reference={error.digest}
          >
            <button onClick={reset} className="rounded-xl border-accent bg-accent px-6 py-3 font-bold text-white hover:bg-accent-hover">
              Try again
            </button>
            {/* a plain link: the app's router may be what broke */}
            <a href="/" className="rounded-xl border border-line-strong bg-surface px-6 py-3 font-semibold text-ink hover:no-underline">
              Go to home
            </a>
          </ErrorScreen>
        </main>
      </body>
    </html>
  );
}
