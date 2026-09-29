'use client';

import { useEffect, useState } from 'react';

const CONFETTI = Array.from({ length: 22 }, (_, i) => ({
  left: `${(i * 37) % 100}%`,
  delay: `${(i % 7) * 0.12}s`,
  duration: `${1.6 + (i % 5) * 0.25}s`,
  color: ['#ffffff', '#ffd166', '#ff7ab6', '#7cf0c9', '#9fb4ff'][i % 5],
  rotate: `${(i * 53) % 360}deg`,
}));

const SHOWN_MS = 2600;

/** A full-screen "order placed" that covers the order page for a moment, then fades away. */
export function OrderPlaced({ number }: { number: string }) {
  const [visible, setVisible] = useState(true);

  useEffect(() => {
    // a refresh of the order page must not celebrate again
    window.history.replaceState(null, '', window.location.pathname);
    const timer = setTimeout(() => setVisible(false), SHOWN_MS);
    return () => clearTimeout(timer);
  }, []);

  if (!visible) return null;

  return (
    <div className="placed-overlay" role="status" onClick={() => setVisible(false)}>
      {CONFETTI.map((piece, i) => (
        <span
          key={i}
          className="confetti"
          style={{ left: piece.left, animationDelay: piece.delay, animationDuration: piece.duration, background: piece.color, rotate: piece.rotate }}
          aria-hidden
        />
      ))}
      <div className="placed-card">
        <svg className="placed-check" viewBox="0 0 52 52" aria-hidden>
          <circle className="placed-circle" cx="26" cy="26" r="24" fill="none" />
          <path className="placed-tick" fill="none" d="M14 27l8 8 16-17" />
        </svg>
        <h2>Order placed!</h2>
        <p>
          Your order <strong className="mono">{number}</strong> is confirmed.
        </p>
      </div>
    </div>
  );
}
