import { AuthForm } from '@/components/AuthForm';
import { AuthLayout } from '@/components/AuthLayout';
import { register } from '@/lib/actions';

export default async function RegisterPage({ searchParams }: PageProps<'/register'>) {
  const { next } = await searchParams;
  return (
    <AuthLayout>
      <AuthForm action={register} mode="register" next={typeof next === 'string' ? next : '/'} />
    </AuthLayout>
  );
}
