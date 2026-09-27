import { ReturnIcon, ShieldIcon, TruckIcon } from './icons';
import { api } from '@/lib/sdk';

/** Six product pictures for the collage. The page still works, with a plain gradient, if the catalog is down. */
async function collage(): Promise<string[]> {
  try {
    const page = await api().catalog.listCatalogProducts({ size: 12, availability: 'AVAILABLE' });
    return page.items.flatMap((product) => product.images.slice(0, 1)).slice(0, 6);
  } catch {
    return [];
  }
}

/** Sign-in and sign-up pages: pictures and a promise on the left (large screens), the form on the right. */
export async function AuthLayout({ children }: { children: React.ReactNode }) {
  const pictures = await collage();

  return (
    <section className="mx-auto my-2 grid w-full max-w-5xl grid-cols-1 overflow-hidden rounded-3xl border border-line bg-surface shadow-card sm:my-6 lg:min-h-[620px] lg:grid-cols-2">
      <aside
        className="relative hidden flex-col justify-between gap-8 overflow-hidden p-10 text-white lg:flex"
        style={{ background: 'var(--hero)' }}
      >
        <div className="relative z-10">
          <p className="text-xs font-bold tracking-[0.14em] uppercase opacity-80">Welcome to Cartly</p>
          <h2 className="mt-2 max-w-[14ch] text-4xl leading-[1.1] font-extrabold tracking-tight">Everything you love, in one cart.</h2>
        </div>

        {pictures.length > 0 && (
          <ul className="relative z-10 grid -rotate-6 grid-cols-3 gap-3 self-center" aria-hidden>
            {pictures.map((src, i) => (
              <li key={src} className={i % 3 === 1 ? 'mt-8' : i % 3 === 2 ? 'mt-3' : ''}>
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src={src}
                  alt=""
                  className="aspect-square w-full rounded-2xl bg-white object-contain p-2 shadow-xl ring-1 ring-white/40"
                />
              </li>
            ))}
          </ul>
        )}

        <ul className="relative z-10 flex flex-wrap gap-x-5 gap-y-2 text-sm font-medium">
          <li className="flex items-center gap-2">
            <TruckIcon size={18} /> Delivery in 3 to 5 days
          </li>
          <li className="flex items-center gap-2">
            <ReturnIcon size={18} /> 30-day returns
          </li>
          <li className="flex items-center gap-2">
            <ShieldIcon size={18} /> Secure checkout
          </li>
        </ul>

        <div className="pointer-events-none absolute -top-24 -right-24 size-80 rounded-full bg-white/20 blur-3xl" />
      </aside>

      <div className="flex items-center justify-center p-6 sm:p-10">{children}</div>
    </section>
  );
}
