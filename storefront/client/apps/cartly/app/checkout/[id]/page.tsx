import { SdkHttpError } from '@shop/platform-sdk';
import Link from 'next/link';
import { notFound } from 'next/navigation';
import { LineItems } from '@/components/LineItems';
import { cancelCheckout, placeOrder } from '@/lib/actions';
import { getSkuIndex } from '@/lib/catalog';
import { formatDateTime } from '@/lib/format';
import { api } from '@/lib/sdk';
import { requireUser } from '@/lib/session';

async function loadCheckout(checkoutId: string) {
  try {
    return await api().checkout.getCheckout({ checkoutId });
  } catch (error) {
    if (error instanceof SdkHttpError && (error.status === 404 || error.status === 400)) notFound();
    throw error;
  }
}

export default async function CheckoutPage({ params, searchParams }: PageProps<'/checkout/[id]'>) {
  const { id } = await params;
  const { error } = await searchParams;
  const user = await requireUser(`/checkout/${id}`);
  const [checkout, index] = await Promise.all([loadCheckout(id), getSkuIndex()]);
  if (checkout.customerId !== user.id) notFound();

  return (
    <>
      <Link href="/cart" className="back">
        ← Back to cart
      </Link>
      <h1>Review your order</h1>
      {typeof error === 'string' && <p className="error">{error}</p>}
      <LineItems items={checkout.items} index={index} total={checkout.totalAmount} />

      {checkout.status === 'DRAFT' ? (
        <div className="summary card">
          <span className="muted">Prices held until {formatDateTime(checkout.expiresAt)}</span>
          <div className="row">
            <form action={cancelCheckout}>
              <input type="hidden" name="checkoutId" value={checkout.checkoutId} />
              <button>Cancel</button>
            </form>
            <form action={placeOrder}>
              <input type="hidden" name="checkoutId" value={checkout.checkoutId} />
              <button className="primary">Place order</button>
            </form>
          </div>
        </div>
      ) : (
        <p className="card">
          This checkout is <strong>{checkout.status.toLowerCase()}</strong>.{' '}
          {checkout.status === 'COMMITTED' ? (
            <Link href={`/orders/${checkout.checkoutId}`}>View order</Link>
          ) : (
            <Link href="/cart">Back to cart</Link>
          )}
        </p>
      )}
    </>
  );
}
