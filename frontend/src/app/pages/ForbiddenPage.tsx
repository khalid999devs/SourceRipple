import { Link } from 'react-router';

import { Button } from '@/components/ui/form/button';
import { useDocumentTitle } from '@/hooks/useDocumentTitle';
import type { Role } from '@/lib/roles';

import { SystemMessage } from './SystemMessage';

interface ForbiddenPageProps {
  role: Role;
}

export function ForbiddenPage({ role }: ForbiddenPageProps) {
  useDocumentTitle('Access denied');

  return (
    <SystemMessage
      code="403"
      title="You don't have access to this page"
      actions={
        <Button asChild variant="secondary">
          <Link to="/app">Back to overview</Link>
        </Button>
      }
    >
      <p>
        This page requires the <span className="font-mono text-foreground">{role}</span> role. Ask
        an administrator if you need it.
      </p>
    </SystemMessage>
  );
}
