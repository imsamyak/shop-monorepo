import { ChatWidget } from '@cartly/chat';
import type { Metadata } from 'next';
import Link from 'next/link';
import { logout } from '@/lib/actions';
import { api } from '@/lib/sdk';
import { getUser } from '@/lib/session';
import './globals.css';

export const metadata: Metadata = {
  title: { default: 'Cartly', template: '%s · Cartly' },
  description: 'Browse, add to cart, check out.',
};

async function cartCount(customerId: string): Promise<number> {
  try {
    const cart = await api().cart.getCart({ customerId });
    return cart.items.reduce((sum, item) => sum + item.quantity, 0);
  } catch {
    return 0;
  }
}

export default async function RootLayout({ children }: { children: React.ReactNode }) {
  const user = await getUser().catch(() => null);
  const count = user ? await cartCount(user.id) : 0;

  return (
    <html lang="en">
      <body>
        <header className="header">
          <div className="container header-inner">
            <Link href="/" className="brand">
              Cartly
            </Link>
            <nav>
              {user?.role === 'ROLE_SELLER' ? <Link href="/seller">Your products</Link> : <Link href="/sell">Sell on Cartly</Link>}
              {user && <Link href="/orders">Orders</Link>}
              <Link href="/cart">Cart{count > 0 && <span className="count">{count}</span>}</Link>
              {user ? (
                <form action={logout}>
                  <button className="link" title={user.email}>
                    Sign out
                  </button>
                </form>
              ) : (
                <Link href="/login">Sign in</Link>
              )}
            </nav>
          </div>
        </header>
        <main className="container">{children}</main>
        {user && <ChatWidget title="Cartly assistant" />}
      </body>
    </html>
  );
}
