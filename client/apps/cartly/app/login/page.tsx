import { AuthForm } from '@/components/AuthForm';
import { login } from '@/lib/actions';

export default async function LoginPage({ searchParams }: PageProps<'/login'>) {
  const { next } = await searchParams;
  return <AuthForm action={login} mode="login" next={typeof next === 'string' ? next : '/'} />;
}
