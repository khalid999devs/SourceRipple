import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { ApiError, toApiError } from '@/lib/api/api-error';
import { api } from '@/lib/api/client';
import { CSRF_HEADER, csrfToken } from '@/lib/api/csrf';
import type { components } from '@/lib/api/schema';

export type Me = components['schemas']['MeResponse'];
export type LoginRequest = components['schemas']['LoginRequest'];
export type RegisterRequest = components['schemas']['RegisterRequest'];

export const authKeys = {
  all: ['auth'] as const,
  me: () => [...authKeys.all, 'me'] as const,
};

async function fetchMe(): Promise<Me | null> {
  try {
    const { data } = await api.GET('/api/v1/auth/me');
    return data ?? null;
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      return null;
    }
    throw error;
  }
}

// Spring Security's logout filter serves this URL, so it is not part of the OpenAPI spec.
async function logout(): Promise<void> {
  const token = await csrfToken();
  const response = await fetch('/api/v1/auth/logout', {
    method: 'POST',
    credentials: 'same-origin',
    headers: token ? { [CSRF_HEADER]: token } : undefined,
  });

  if (!response.ok) {
    throw await toApiError(response);
  }
}

export function useMe() {
  return useQuery({
    queryKey: authKeys.me(),
    queryFn: fetchMe,
    staleTime: 5 * 60_000,
  });
}

export function useLogin() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (body: LoginRequest) => {
      const { data } = await api.POST('/api/v1/auth/login', { body });
      return data;
    },
    onSuccess: (me) => {
      queryClient.setQueryData(authKeys.me(), me);
    },
  });
}

export function useRegister() {
  return useMutation({
    mutationFn: async (body: RegisterRequest) => {
      const { data } = await api.POST('/api/v1/auth/register', { body });
      return data;
    },
  });
}

export function useLogout() {
  return useMutation({
    mutationFn: logout,
    onSuccess: () => {
      // A full reload drops every cached query and in-memory state from the old session.
      window.location.replace('/login');
    },
  });
}
