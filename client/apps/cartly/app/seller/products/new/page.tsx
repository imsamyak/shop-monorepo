import Link from 'next/link';
import { NewProductForm } from '@/components/NewProductForm';
import { requireSeller } from '@/lib/session';

export const metadata = { title: 'New product' };

export default async function NewProductPage() {
  await requireSeller('/seller/products/new');
  return (
    <>
      <Link href="/seller" className="back">
        ← Your products
      </Link>
      <NewProductForm />
    </>
  );
}
