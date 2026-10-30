import { ChatWidget } from '@cartly/chat';
import type { Metadata } from 'next';
import Link from 'next/link';
import { CartLink } from '@/components/CartLink';
import { HeaderSearch } from '@/components/HeaderSearch';
import { LogoutButton } from '@/components/LogoutButton';
import { StoreProvider } from '@/components/StoreProvider';
import { getUser } from '@/lib/session';
import './globals.css';

export const metadata: Metadata = {
  title: { default: 'Cartly', template: '%s · Cartly' },
  description: 'Browse, add to cart, check out.',
};

export default async function RootLayout({ children }: { children: React.ReactNode }) {
  const user = await getUser().catch(() => null);

  return (
    <html lang="en">
      <body>
        <StoreProvider>
          <header className="header">
            <div className="page header-inner">
              <Link href="/" className="brand">
                <span className="logo" aria-hidden>
                  C
                </span>
                Cartly
              </Link>
              <HeaderSearch />
              <nav>
                <Link href="/arch">Architecture</Link>
                {user && <Link href="/orders">Orders</Link>}
                <CartLink signedIn={Boolean(user)} />
                {user ? (
                  <LogoutButton email={user.email} />
                ) : (
                  <>
                    <Link href="/login">Sign in</Link>
                    <Link href="/register" className="button primary small">
                      Sign up
                    </Link>
                  </>
                )}
              </nav>
            </div>
          </header>
          <main className="page">{children}</main>
          <footer className="footer">
            <div className="page">Cartly · Browse, add to cart, check out.</div>
          </footer>
          {user && <ChatWidget title="Cartly assistant" />}
        </StoreProvider>
      </body>
    </html>
  );
}
