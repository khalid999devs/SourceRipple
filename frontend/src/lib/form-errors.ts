import type { FieldValues, Path, UseFormSetError } from 'react-hook-form';

import { ApiError } from '@/lib/api/api-error';

export function applyFieldErrors<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
  fields: readonly Path<T>[],
): void {
  if (!(error instanceof ApiError)) {
    return;
  }

  for (const { field, message } of error.fieldErrors) {
    const name = fields.find((candidate) => candidate === field);
    if (name) {
      setError(name, { type: 'server', message });
    }
  }
}
