import type { ComponentProps } from 'react';

import { Input } from '@/components/ui/form/input';
import { Label } from '@/components/ui/form/label';

interface FormFieldProps extends ComponentProps<typeof Input> {
  id: string;
  label: string;
  error?: string;
  hint?: string;
}

export function FormField({ id, label, error, hint, ...inputProps }: FormFieldProps) {
  const messageId = `${id}-message`;
  const message = error ?? hint;

  return (
    <div className="flex flex-col gap-2">
      <Label htmlFor={id}>{label}</Label>
      <Input
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={message ? messageId : undefined}
        {...inputProps}
      />
      {message && (
        <p
          id={messageId}
          className={error ? 'text-xs text-destructive' : 'text-xs text-muted-foreground'}
        >
          {message}
        </p>
      )}
    </div>
  );
}
