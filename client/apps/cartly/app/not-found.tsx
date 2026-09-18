import Link from 'next/link';

export default function NotFound() {
  return (
    <div className="card center">
      <h1>Not found</h1>
      <p className="muted">We couldn’t find that page.</p>
      <Link href="/">Back to the shop</Link>
    </div>
  );
}
