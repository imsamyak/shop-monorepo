import { SdkHttpError } from '@shop/platform-sdk';
import Link from 'next/link';
import { notFound } from 'next/navigation';
import { LineItems, OrderSummary } from '@/components/LineItems';
import { OrderPlaced } from '@/components/OrderPlaced';
import { getSkuIndex } from '@/lib/catalog';
import { orderNumber } from '@/lib/format';
import { api } from '@/lib/sdk';
import { requireUser } from '@/lib/session';

const HEADINGS: Record<string, string> = {
  COMMITTED: 'Order Placed',
  CANCELLED: 'Order Cancelled',
  DRAFT: 'Order Pending',
};

async function loadOrder(orderId: string) {
  try {
    return await api().orders.getOrder({ orderId });
  } catch (error) {
    if (error instanceof SdkHttpError && (error.status === 404 || error.status === 400)) notFound();
    throw error;
  }
}

export default async function OrderPage({ params, searchParams }: PageProps<'/orders/[id]'>) {
  const { id } = await params;
  const { placed } = await searchParams;
  const user = await requireUser(`/orders/${id}`);
  const [order, index] = await Promise.all([loadOrder(id), getSkuIndex()]);
  if (order.customerId !== user.id) notFound();

  return (
    <>
      <Link href="/orders" className="back">
        ← All orders
      </Link>
      {placed === '1' && <OrderPlaced number={orderNumber(order.orderId)} />}
      <div className="order-head">
        <h1>{HEADINGS[order.status] ?? 'Your order'}</h1>
        <span className="order-id">{orderNumber(order.orderId)}</span>
      </div>
      <div className="cart-layout">
        <LineItems items={order.items} index={index} />
        <OrderSummary title="Order summary" items={order.items} total={order.totalAmount} />
      </div>
    </>
  );
}
