import { MutationCache, QueryCache, QueryClient } from '@tanstack/react-query';

import { ApiError } from '@/lib/api/api-error';
import { notifyError } from '@/lib/notify';

const MAX_RETRIES = 2;

function shouldRetry(failureCount: number, error: Error): boolean {
  if (error instanceof ApiError && !error.retryable && error.status < 500) {
    return false;
  }
  return failureCount < MAX_RETRIES;
}

export function createQueryClient(): QueryClient {
  return new QueryClient({
    queryCache: new QueryCache({
      onError: (error, query) => {
        if (query.state.data !== undefined) {
          notifyError(error);
        }
      },
    }),
    mutationCache: new MutationCache({
      onError: (error) => {
        if (!(error instanceof ApiError) || error.fieldErrors.length === 0) {
          notifyError(error);
        }
      },
    }),
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        retry: shouldRetry,
        refetchOnWindowFocus: false,
      },
      mutations: {
        retry: false,
      },
    },
  });
}
