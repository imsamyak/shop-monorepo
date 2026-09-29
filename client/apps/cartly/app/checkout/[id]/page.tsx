import { SdkHttpError } from '@shop/platform-sdk';
import Link from 'next/link';
import { notFound } from 'next/navigation';
import { LineItems, OrderSummary } from '@/components/LineItems';
import { cancelCheckout, placeOrder } from '@/lib/actions';
import { getSkuIndex } from '@/lib/catalog';
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
      {checkout.status === 'DRAFT' ? (
        <div className="cart-layout">
          <LineItems items={checkout.items} index={index} />
          <OrderSummary title="Order summary" items={checkout.items} total={checkout.totalAmount}>
            <form action={placeOrder}>
              <input type="hidden" name="checkoutId" value={checkout.checkoutId} />
              <button className="primary wide">Place order</button>
            </form>
            <form action={cancelCheckout}>
              <input type="hidden" name="checkoutId" value={checkout.checkoutId} />
              <button className="wide">Cancel</button>
            </form>
          </OrderSummary>
        </div>
      ) : (
        <>
          <LineItems items={checkout.items} index={index} />
          <p className="card">
            This checkout is <strong>{checkout.status.toLowerCase()}</strong>.{' '}
            {checkout.status === 'COMMITTED' ? (
              <Link href={`/orders/${checkout.checkoutId}`}>View order</Link>
            ) : (
              <Link href="/cart">Back to cart</Link>
            )}
          </p>
        </>
      )}
    </>
  );
}
