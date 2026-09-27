import { CartView } from '@/components/CartView';
import { requireUser } from '@/lib/session';

export default async function CartPage({ searchParams }: PageProps<'/cart'>) {
  await requireUser('/cart');
  const { error } = await searchParams;
  return (
    <>
      <h1>Your cart</h1>
      <CartView error={typeof error === 'string' ? error : undefined} />
    </>
  );
}
