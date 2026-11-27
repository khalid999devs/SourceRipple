import { useQueryClient } from '@tanstack/react-query';
import { WifiOff } from 'lucide-react';

import { Button } from '@/components/ui/form/button';
import { useOnlineStatus } from '@/hooks/useOnlineStatus';

export function OfflineBanner() {
  const online = useOnlineStatus();
  const queryClient = useQueryClient();

  if (online) {
    return null;
  }

  return (
    <div
      role="status"
      aria-live="polite"
      className="flex items-center justify-center gap-3 border-b border-amber-400/30 bg-amber-400/10 px-4 py-2 text-sm text-amber-200"
    >
      <WifiOff aria-hidden className="size-4 shrink-0" />
      <span>You are offline. Data may be out of date until the connection returns.</span>
      <Button
        size="sm"
        variant="ghost"
        className="h-7 text-amber-200 hover:text-amber-100"
        onClick={() => queryClient.refetchQueries({ type: 'active' })}
      >
        Retry
      </Button>
    </div>
  );
}
