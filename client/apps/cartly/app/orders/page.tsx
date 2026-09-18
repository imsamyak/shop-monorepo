import Link from 'next/link';
import { formatMoney } from '@/lib/format';
import { api } from '@/lib/sdk';
import { requireUser } from '@/lib/session';

export default async function OrdersPage({ searchParams }: PageProps<'/orders'>) {
  const user = await requireUser('/orders');
  const page = Math.max(0, Number((await searchParams).page) || 0);
  const orders = await api().orders.listOrders({ customerId: user.id, status: 'COMMITTED', page, size: 20 });

  return (
    <>
      <h1>Your orders</h1>
      {orders.items.length === 0 ? (
        <p className="empty">
          No orders yet. <Link href="/">Start shopping</Link>
        </p>
      ) : (
        <ul className="orders">
          {orders.items.map((order) => (
            <li key={order.orderId}>
              <Link href={`/orders/${order.orderId}`} className="card order-row">
                <span className="mono">#{order.orderId.slice(0, 8)}</span>
                <span className="muted">
                  {order.items.reduce((sum, item) => sum + item.quantity, 0)} item(s)
                </span>
                <strong>{formatMoney(order.totalAmount)}</strong>
              </Link>
            </li>
          ))}
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
