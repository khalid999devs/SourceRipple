import { useQuery } from '@tanstack/react-query';

import type { Role } from '@/lib/roles';

export interface MeOrganization {
  id: string;
  name: string;
  owner: boolean;
}

export interface Me {
  user: {
    id: string;
    email: string;
    displayName: string;
  };
  roles: Role[];
  organizations: MeOrganization[];
}

export const authKeys = {
  all: ['auth'] as const,
  me: () => [...authKeys.all, 'me'] as const,
};

async function fetchMe(): Promise<Me | null> {
  const response = await fetch('/api/v1/auth/me', {
    credentials: 'same-origin',
    headers: { Accept: 'application/json' },
  });

  if (response.status === 401) {
    return null;
  }
  if (!response.ok) {
    throw new Error(`Loading the current user failed with status ${response.status}`);
  }
  return (await response.json()) as Me;
}

export function useMe() {
  return useQuery({
    queryKey: authKeys.me(),
    queryFn: fetchMe,
    staleTime: 5 * 60_000,
  });
}
