import Link from 'next/link';
import { getSkuIndex } from '@/lib/catalog';
import { formatDateTime, formatMoney, orderNumber } from '@/lib/format';
import { mainImage } from '@/lib/images';
import { api } from '@/lib/sdk';
import { requireUser } from '@/lib/session';

const THUMBNAILS = 3;

export default async function OrdersPage({ searchParams }: PageProps<'/orders'>) {
  const user = await requireUser('/orders');
  const page = Math.max(0, Number((await searchParams).page) || 0);
  const [orders, index] = await Promise.all([
    api().orders.listOrders({ customerId: user.id, status: 'COMMITTED', page, size: 20 }),
    getSkuIndex(),
  ]);

  return (
    <>
      <h1 className="page-title">Your orders</h1>
      {orders.items.length === 0 ? (
        <div className="empty-state">
          <h2>No orders yet</h2>
          <p className="muted">When you place an order, it will show up here.</p>
          <Link href="/" className="button primary">
            Start shopping
          </Link>
        </div>
      ) : (
        <ul className="orders">
          {orders.items.map((order) => {
            // one thumbnail per product, in the order the lines come
            const products = new Map<string, string>();
            for (const item of order.items) {
              const info = index.get(item.skuId);
              if (info && !products.has(info.product.id)) products.set(info.product.id, mainImage(info.product.images));
            }
            const pictures = [...products.values()];
            const more = pictures.length - THUMBNAILS;
            const units = order.items.reduce((sum, item) => sum + item.quantity, 0);

            return (
              <li key={order.orderId}>
                <Link href={`/orders/${order.orderId}`} className="order-card">
                  <div className="order-thumbs" aria-hidden>
                    {pictures.slice(0, THUMBNAILS).map((src, i) => (
                      // eslint-disable-next-line @next/next/no-img-element
                      <img key={i} src={src} alt="" />
                    ))}
                    {more > 0 && <span className="more">+{more}</span>}
                  </div>
                  <div className="order-main">
                    <strong className="mono">{orderNumber(order.orderId)}</strong>
                    <span className="muted small">
                      {order.createdAt ? `${formatDateTime(order.createdAt)} · ` : ''}
                      {units} {units === 1 ? 'item' : 'items'}
                    </span>
                  </div>
                  <strong className="order-total">{formatMoney(order.totalAmount)}</strong>
                  <span className="order-chevron" aria-hidden>
                    ›
                  </span>
                </Link>
              </li>
            );
          })}
        </ul>
      )}
      {orders.totalPages > 1 && (
        <nav className="pager">
          {page > 0 ? <Link href={`/orders?page=${page - 1}`}>← Newer</Link> : <span />}
          <span className="muted">
            Page {page + 1} of {orders.totalPages}
          </span>
          {page + 1 < orders.totalPages ? <Link href={`/orders?page=${page + 1}`}>Older →</Link> : <span />}
        </nav>
      )}
    </>
  );
}
