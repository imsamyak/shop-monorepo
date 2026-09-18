import Link from 'next/link';
import { formatMoney } from '@/lib/format';
import { mainImage } from '@/lib/images';
import { api } from '@/lib/sdk';
import { requireSeller } from '@/lib/session';

export const metadata = { title: 'Your products' };

export default async function SellerPage({ searchParams }: PageProps<'/seller'>) {
  const user = await requireSeller('/seller');
  const page = Math.max(0, Number((await searchParams).page) || 0);
  const products = await api().products.listProducts({ sellerId: user.sellerId, page, size: 20, sort: 'createdAt,desc' });

  return (
    <>
      <div className="toolbar">
        <h1>Your products</h1>
        <Link href="/seller/products/new" className="button primary">
          New product
        </Link>
      </div>

      {products.items.length === 0 ? (
        <p className="empty">
          You have not listed anything yet. <Link href="/seller/products/new">List your first product</Link>
        </p>
      ) : (
        <ul className="orders">
          {products.items.map((product) => (
            <li key={product.id}>
              <Link href={`/seller/products/${product.id}`} className="card order-row">
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img src={mainImage(product.images)} alt="" className="thumb" />
                <span className="grow">{product.title}</span>
                <span className="muted">{product.images.length} picture(s)</span>
                <strong>{formatMoney(product.defaultPrice)}</strong>
              </Link>
            </li>
          ))}
        </ul>
      )}

      {products.totalPages > 1 && (
        <nav className="pager">
          {page > 0 ? <Link href={`/seller?page=${page - 1}`}>← Newer</Link> : <span />}
          <span className="muted">
            Page {page + 1} of {products.totalPages}
          </span>
          {page + 1 < products.totalPages ? <Link href={`/seller?page=${page + 1}`}>Older →</Link> : <span />}
        </nav>
      )}
    </>
  );
}
