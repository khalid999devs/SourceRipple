import { toast } from 'sonner';

import { ApiError } from '@/lib/api/api-error';

export function notifyError(error: unknown): void {
  if (error instanceof ApiError) {
    toast.error(error.message, {
      description: error.requestId ? `Request ID: ${error.requestId}` : undefined,
    });
    return;
  }

  toast.error('Something went wrong. Please try again.');
}
