import { SdkHttpError } from '@shop/platform-sdk';
import Link from 'next/link';
import { notFound } from 'next/navigation';
import { LineItems } from '@/components/LineItems';
import { getSkuIndex } from '@/lib/catalog';
import { orderNumber } from '@/lib/format';
import { api } from '@/lib/sdk';
import { requireUser } from '@/lib/session';

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
      {placed === '1' && <p className="success card">Thanks! Your order has been placed.</p>}
      <h1>
        Order <span className="mono">{orderNumber(order.orderId)}</span>{' '}
        <span className="badge">{order.status.toLowerCase()}</span>
      </h1>
      <LineItems items={order.items} index={index} total={order.totalAmount} />
    </>
  );
}
