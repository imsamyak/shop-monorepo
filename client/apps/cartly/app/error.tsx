'use client';

export default function Error({ reset }: { error: Error; reset: () => void }) {
  return (
    <div className="card center">
      <h1>Something went wrong</h1>
      <p className="muted">The shop could not load this page. The services may be down.</p>
      <button className="primary" onClick={reset}>
        Try again
      </button>
    </div>
  );
}
