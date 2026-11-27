import { Button } from '@/components/ui/form/button';
import { useDocumentTitle } from '@/hooks/useDocumentTitle';

import { SystemMessage } from './SystemMessage';

interface ServerErrorPageProps {
  requestId?: string;
}

export function ServerErrorPage({ requestId }: ServerErrorPageProps) {
  useDocumentTitle('Something went wrong');

  return (
    <SystemMessage
      code="500"
      title="Something went wrong"
      actions={<Button onClick={() => window.location.reload()}>Try again</Button>}
    >
      <p>An unexpected error stopped this page from loading. Trying again usually helps.</p>
      {requestId && (
        <p className="text-sm">
          Request ID{' '}
          <code className="rounded bg-surface-2 px-1.5 py-0.5 font-mono text-foreground select-all">
            {requestId}
          </code>
        </p>
      )}
    </SystemMessage>
  );
}
