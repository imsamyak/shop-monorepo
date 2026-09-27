import { SdkHttpError } from '@shop/platform-sdk';
import Link from 'next/link';
import { notFound } from 'next/navigation';
import { AddToCart } from '@/components/AddToCart';
import { Gallery } from '@/components/Gallery';
import { ReturnIcon, ShieldIcon, TruckIcon } from '@/components/icons';
import { skuLabel } from '@/lib/catalog';
import { api } from '@/lib/sdk';

async function loadProduct(id: string) {
  try {
    return await api().catalog.getCatalogProduct({ id });
  } catch (error) {
    if (error instanceof SdkHttpError && error.status === 404) notFound();
    throw error;
  }
}

export default async function ProductPage({ params }: PageProps<'/products/[id]'>) {
  const { id } = await params;
  const product = await loadProduct(id);

  return (
    <>
      <Link href="/" className="back">
        ← All products
      </Link>
      <article className="product">
        <Gallery images={product.images} title={product.title} />
        <div className="product-info">
          <h1>{product.title}</h1>
          <p className="product-description">{product.description}</p>
          <AddToCart
            productId={product.id}
            options={product.skus.map((sku) => ({
              skuId: Number(sku.skuId),
              label: skuLabel(sku),
              price: sku.price,
              available: sku.availability === 'AVAILABLE',
            }))}
          />
          <ul className="perks">
            <li>
              <TruckIcon /> Standard delivery in 3 to 5 business days
            </li>
            <li>
              <ReturnIcon /> Easy returns within 30 days
            </li>
            <li>
              <ShieldIcon /> Secure checkout
            </li>
          </ul>
        </div>
      </article>
    </>
  );
}
