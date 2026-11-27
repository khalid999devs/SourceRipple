import { Slot } from '@radix-ui/react-slot';
import type { ComponentProps } from 'react';

import { cn } from '@/lib/utils';

import { badgeVariants, type BadgeVariantProps } from './badge-variants';

type BadgeProps = ComponentProps<'span'> &
  BadgeVariantProps & {
    asChild?: boolean;
  };

export function Badge({ className, variant, asChild = false, ...props }: BadgeProps) {
  const Component = asChild ? Slot : 'span';
  return (
    <Component data-slot="badge" className={cn(badgeVariants({ variant }), className)} {...props} />
  );
}
