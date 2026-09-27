'use client';

import Link from 'next/link';
import { AlertIcon } from '@/components/icons';
import { ErrorScreen } from '@/components/ErrorScreen';

export default function Error({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return (
    <ErrorScreen
      icon={<AlertIcon />}
      eyebrow="Something went wrong"
      title="Sorry, something went wrong on our end."
      message="Please try again in a moment, or head back to the Cartly home page."
      reference={error.digest}
    >
      <button onClick={reset} className="rounded-xl border-accent bg-accent px-6 py-3 font-bold text-white hover:bg-accent-hover">
        Try again
      </button>
      <Link href="/" className="rounded-xl border border-line-strong bg-surface px-6 py-3 font-semibold text-ink hover:no-underline">
        Go to home
      </Link>
    </ErrorScreen>
  );
}
