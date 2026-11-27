import { Navigate, Outlet, useLocation } from 'react-router';

import { RouteFallback } from '@/components/shared/RouteFallback';
import { useMe } from '@/features/auth/api';

const ONBOARDING_PATH = '/app/onboarding';

export function RequireAuth() {
  const { pathname, search } = useLocation();
  const { data: me, isPending, isError, error } = useMe();

  if (isPending) {
    return <RouteFallback />;
  }
  if (isError) {
    throw error;
  }
  if (!me) {
    const next = encodeURIComponent(pathname + search);
    return <Navigate to={`/login?next=${next}`} replace />;
  }
  if (!me.organizations?.length && pathname !== ONBOARDING_PATH) {
    return <Navigate to={ONBOARDING_PATH} replace />;
  }

  return <Outlet />;
}
