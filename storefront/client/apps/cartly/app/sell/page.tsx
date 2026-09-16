import { AuthForm } from '@/components/AuthForm';
import { registerSeller } from '@/lib/seller-actions';

export const metadata = { title: 'Sell on Cartly' };

export default async function SellPage({ searchParams }: PageProps<'/sell'>) {
  const { next } = await searchParams;
  return <AuthForm action={registerSeller} mode="seller" next={typeof next === 'string' ? next : '/seller'} />;
}
