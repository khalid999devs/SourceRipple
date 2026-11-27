import { Outlet } from 'react-router';

import { ForbiddenPage } from '@/app/pages/ForbiddenPage';
import { useMe } from '@/features/auth/api';
import { hasRole, type Role } from '@/lib/roles';

interface RequireRoleProps {
  role: Role;
}

export function RequireRole({ role }: RequireRoleProps) {
  const { data: me } = useMe();

  if (!hasRole(me?.roles, role)) {
    return <ForbiddenPage role={role} />;
  }

  return <Outlet />;
}
