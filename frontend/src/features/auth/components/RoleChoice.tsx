import { ClipboardCheck, Wrench, type LucideIcon } from 'lucide-react';
import type { ComponentProps } from 'react';

import type { RegisterValues } from '@/features/auth/schemas';

interface RoleOption {
  value: RegisterValues['accountType'];
  title: string;
  description: string;
  icon: LucideIcon;
}

const OPTIONS: readonly RoleOption[] = [
  {
    value: 'MAINTAINER',
    title: 'I maintain projects',
    description: 'Create organizations and projects, add watches and connect webhooks.',
    icon: Wrench,
  },
  {
    value: 'REVIEWER',
    title: 'I review impacts',
    description: 'Triage flagged changes in the organizations you are added to.',
    icon: ClipboardCheck,
  },
];

interface RoleChoiceProps extends Omit<ComponentProps<'input'>, 'type' | 'value'> {
  error?: string;
}

export function RoleChoice({ error, ...inputProps }: RoleChoiceProps) {
  return (
    <fieldset className="flex flex-col gap-2">
      <legend className="mb-2 text-sm font-medium">How will you use SourceRipple?</legend>
      <div className="grid gap-3 sm:grid-cols-2">
        {OPTIONS.map(({ value, title, description, icon: Icon }) => (
          <label
            key={value}
            className="flex cursor-pointer flex-col gap-2 rounded-lg border bg-surface-1 p-4 shadow-card transition-colors duration-150 hover:bg-surface-2 has-checked:border-primary has-checked:bg-primary/5 has-focus-visible:ring-2 has-focus-visible:ring-ring"
          >
            <input type="radio" value={value} className="sr-only" {...inputProps} />
            <Icon aria-hidden className="size-5 text-primary" />
            <span className="font-medium">{title}</span>
            <span className="text-xs text-muted-foreground">{description}</span>
          </label>
        ))}
      </div>
      {error && <p className="text-xs text-destructive">{error}</p>}
    </fieldset>
  );
}
