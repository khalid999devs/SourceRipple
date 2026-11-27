import { QueryClientProvider } from '@tanstack/react-query';
import { MotionConfig } from 'motion/react';
import { useState, type ReactNode } from 'react';

import { Toaster } from '@/components/ui/feedback/sonner';
import { TooltipProvider } from '@/components/ui/overlay/tooltip';

import { createQueryClient } from './query-client';

export function Providers({ children }: { children: ReactNode }) {
  const [queryClient] = useState(createQueryClient);

  return (
    <QueryClientProvider client={queryClient}>
      <TooltipProvider>
        <MotionConfig reducedMotion="user">{children}</MotionConfig>
      </TooltipProvider>
      <Toaster richColors closeButton />
    </QueryClientProvider>
  );
}
