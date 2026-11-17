import { Outlet } from 'react-router';

import type { Role } from '@/lib/roles';

interface RequireRoleProps {
  role: Role;
}

export function RequireRole({ role }: RequireRoleProps) {
  return <Outlet context={{ requiredRole: role }} />;
}
