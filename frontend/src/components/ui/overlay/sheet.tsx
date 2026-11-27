import * as SheetPrimitive from '@radix-ui/react-dialog';
import { X } from 'lucide-react';
import type { ComponentProps } from 'react';

import { cn } from '@/lib/utils';

type SheetSide = 'right' | 'left' | 'bottom';

const SIDE_CLASSES: Record<SheetSide, string> = {
  right:
    'inset-y-0 right-0 h-full w-3/4 border-l sm:max-w-md data-[state=closed]:animate-sheet-out-right data-[state=open]:animate-sheet-in-right',
  left: 'inset-y-0 left-0 h-full w-3/4 border-r sm:max-w-sm data-[state=closed]:animate-sheet-out-left data-[state=open]:animate-sheet-in-left',
  bottom:
    'inset-x-0 bottom-0 max-h-[85dvh] rounded-t-lg border-t data-[state=closed]:animate-sheet-out-bottom data-[state=open]:animate-sheet-in-bottom',
};

export const Sheet = SheetPrimitive.Root;
export const SheetTrigger = SheetPrimitive.Trigger;
export const SheetClose = SheetPrimitive.Close;

function SheetOverlay({ className, ...props }: ComponentProps<typeof SheetPrimitive.Overlay>) {
  return (
    <SheetPrimitive.Overlay
      data-slot="sheet-overlay"
      className={cn(
        'fixed inset-0 z-50 bg-black/60 data-[state=closed]:animate-fade-out data-[state=open]:animate-fade-in',
        className,
      )}
      {...props}
    />
  );
}

interface SheetContentProps extends ComponentProps<typeof SheetPrimitive.Content> {
  side?: SheetSide;
}

export function SheetContent({ className, children, side = 'right', ...props }: SheetContentProps) {
  return (
    <SheetPrimitive.Portal>
      <SheetOverlay />
      <SheetPrimitive.Content
        data-slot="sheet-content"
        className={cn(
          'fixed z-50 flex flex-col gap-4 bg-surface-1 shadow-card',
          SIDE_CLASSES[side],
          className,
        )}
        {...props}
      >
        {children}
        <SheetPrimitive.Close className="absolute top-4 right-4 rounded-sm text-muted-foreground transition-colors hover:text-foreground">
          <X aria-hidden className="size-4" />
          <span className="sr-only">Close</span>
        </SheetPrimitive.Close>
      </SheetPrimitive.Content>
    </SheetPrimitive.Portal>
  );
}

export function SheetHeader({ className, ...props }: ComponentProps<'div'>) {
  return (
    <div
      data-slot="sheet-header"
      className={cn('flex flex-col gap-1.5 p-4', className)}
      {...props}
    />
  );
}

export function SheetFooter({ className, ...props }: ComponentProps<'div'>) {
  return (
    <div
      data-slot="sheet-footer"
      className={cn('mt-auto flex flex-col gap-2 p-4', className)}
      {...props}
    />
  );
}

export function SheetTitle({ className, ...props }: ComponentProps<typeof SheetPrimitive.Title>) {
  return (
    <SheetPrimitive.Title
      data-slot="sheet-title"
      className={cn('font-semibold tracking-tight', className)}
      {...props}
    />
  );
}

export function SheetDescription({
  className,
  ...props
}: ComponentProps<typeof SheetPrimitive.Description>) {
  return (
    <SheetPrimitive.Description
      data-slot="sheet-description"
      className={cn('text-sm text-muted-foreground', className)}
      {...props}
    />
  );
}
