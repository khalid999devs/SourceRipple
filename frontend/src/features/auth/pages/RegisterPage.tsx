import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Link, useLocation } from 'react-router';

import { FormField } from '@/components/shared/FormField';
import { Button } from '@/components/ui/form/button';
import { useLogin, useRegister } from '@/features/auth/api';
import { AuthLayout } from '@/features/auth/components/AuthLayout';
import { RoleChoice } from '@/features/auth/components/RoleChoice';
import { registerSchema, type RegisterValues } from '@/features/auth/schemas';
import { useDocumentTitle } from '@/hooks/useDocumentTitle';
import { ApiError } from '@/lib/api/api-error';
import { applyFieldErrors } from '@/lib/form-errors';

const FIELDS = ['displayName', 'email', 'password', 'accountType'] as const;

export function RegisterPage() {
  useDocumentTitle('Create your account');
  const { search } = useLocation();
  const register = useRegister();
  const login = useLogin();

  const form = useForm<RegisterValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { displayName: '', email: '', password: '' },
  });
  const { errors, isSubmitting } = form.formState;

  const onSubmit = form.handleSubmit(async (values) => {
    try {
      await register.mutateAsync(values);
      await login.mutateAsync({ email: values.email, password: values.password });
    } catch (error) {
      applyFieldErrors(error, form.setError, FIELDS);
      if (error instanceof ApiError && error.error === 'EMAIL_TAKEN') {
        form.setError('email', { type: 'server', message: error.message });
      }
    }
  });

  return (
    <AuthLayout
      title="Create your account"
      description="Free and open source. No credit card, no tracking."
      footer={
        <>
          Already have an account?{' '}
          <Link to={`/login${search}`} className="text-primary hover:underline">
            Log in
          </Link>
        </>
      }
    >
      <form noValidate onSubmit={onSubmit} className="flex flex-col gap-4">
        <RoleChoice error={errors.accountType?.message} {...form.register('accountType')} />
        <FormField
          id="displayName"
          label="Name"
          autoComplete="name"
          error={errors.displayName?.message}
          {...form.register('displayName')}
        />
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
          autoComplete="new-password"
          hint="At least 10 characters."
          error={errors.password?.message}
          {...form.register('password')}
        />
        <Button type="submit" disabled={isSubmitting} className="mt-2">
          {isSubmitting ? 'Creating account…' : 'Create account'}
        </Button>
      </form>
    </AuthLayout>
  );
}
