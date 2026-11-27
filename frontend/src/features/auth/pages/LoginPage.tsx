import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Link, useLocation } from 'react-router';

import { FormField } from '@/components/shared/FormField';
import { Button } from '@/components/ui/form/button';
import { useLogin } from '@/features/auth/api';
import { AuthLayout } from '@/features/auth/components/AuthLayout';
import { loginSchema, type LoginValues } from '@/features/auth/schemas';
import { useDocumentTitle } from '@/hooks/useDocumentTitle';
import { applyFieldErrors } from '@/lib/form-errors';

const FIELDS = ['email', 'password'] as const;

export function LoginPage() {
  useDocumentTitle('Log in');
  const { search } = useLocation();
  const login = useLogin();

  const form = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  });
  const { errors, isSubmitting } = form.formState;

  const onSubmit = form.handleSubmit(async (values) => {
    try {
      await login.mutateAsync(values);
    } catch (error) {
      applyFieldErrors(error, form.setError, FIELDS);
    }
  });

  return (
    <AuthLayout
      title="Log in"
      description="Welcome back. Pick up where your watches left off."
      footer={
        <>
          New to SourceRipple?{' '}
          <Link to={`/register${search}`} className="text-primary hover:underline">
            Create an account
          </Link>
        </>
      }
    >
      <form noValidate onSubmit={onSubmit} className="flex flex-col gap-4">
        <FormField
          id="email"
          label="Email"
          type="email"
          autoComplete="email"
          error={errors.email?.message}
          {...form.register('email')}
        />
        <FormField
          id="password"
          label="Password"
          type="password"
          autoComplete="current-password"
          error={errors.password?.message}
          {...form.register('password')}
        />
        <Button type="submit" disabled={isSubmitting} className="mt-2">
          {isSubmitting ? 'Logging in…' : 'Log in'}
        </Button>
      </form>
    </AuthLayout>
  );
}
