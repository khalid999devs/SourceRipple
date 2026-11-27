import { Link, useLocation } from 'react-router';

import { Button } from '@/components/ui/form/button';
import { useDocumentTitle } from '@/hooks/useDocumentTitle';

import { SystemMessage } from './SystemMessage';

export function NotFoundPage() {
  useDocumentTitle('Page not found');
  const { pathname } = useLocation();
  const insideApp = pathname.startsWith('/app');

  return (
    <SystemMessage
      code="404"
      title="Page not found"
      actions={
        <Button asChild variant="secondary">
          <Link to={insideApp ? '/app' : '/'}>
            {insideApp ? 'Back to overview' : 'Back to home'}
          </Link>
        </Button>
      }
    >
      <p>The page you are looking for does not exist or has moved.</p>
    </SystemMessage>
  );
}
