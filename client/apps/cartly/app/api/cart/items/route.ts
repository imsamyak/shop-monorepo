import { forUser, toCartView } from '@/lib/api-route';
import { api } from '@/lib/sdk';

/** Adds to the quantity of a SKU's line in the signed-in buyer's cart. */
export async function POST(request: Request) {
  const input = (await request.json().catch(() => null)) as { skuId?: unknown; quantity?: unknown } | null;
  const skuId = Number(input?.skuId);
  const quantity = Number(input?.quantity ?? 1);
  if (!Number.isInteger(skuId) || skuId < 1 || !Number.isInteger(quantity) || quantity < 1 || quantity > 1000) {
    return Response.json({ error: 'Pick an option and a quantity first.' }, { status: 400 });
  }
  return forUser(async (user) => toCartView(await api().cart.addCartItem({ customerId: user.id, skuId, quantity })));
}
