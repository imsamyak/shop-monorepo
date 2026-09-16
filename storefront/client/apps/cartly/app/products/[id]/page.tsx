import { SdkHttpError } from '@shop/platform-sdk';
import Link from 'next/link';
import { notFound } from 'next/navigation';
import { AddToCart } from '@/components/AddToCart';
import { skuLabel } from '@/lib/catalog';
import { formatMoney } from '@/lib/format';
import { PLACEHOLDER_IMAGE } from '@/lib/images';
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
        <div className="gallery">
          {/* The first image is the main picture; the rest follow in the order the seller gave them. */}
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src={product.images[0] ?? PLACEHOLDER_IMAGE} alt="" />
          {product.images.length > 1 && (
            <ul className="thumbs">
              {product.images.map((url, position) => (
                <li key={url}>
                  <a href={url} target="_blank" rel="noreferrer" aria-label={`Picture ${position + 1}`}>
                    {/* eslint-disable-next-line @next/next/no-img-element */}
                    <img src={url} alt="" loading="lazy" />
                  </a>
                </li>
              ))}
            </ul>
          )}
        </div>
        <div>
          <h1>{product.title}</h1>
          <p className="price big">{formatMoney(product.lowestSkuPrice ?? product.price)}</p>
          <p>{product.description}</p>
          <AddToCart
            productId={product.id}
            options={product.skus.map((sku) => ({
              skuId: sku.skuId,
              label: skuLabel(sku),
              price: formatMoney(sku.price),
              available: sku.availability === 'AVAILABLE',
            }))}
          />
        </div>
      </article>
    </>
  );
}
