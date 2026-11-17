import { Link } from 'react-router';

import { Button } from '@/components/ui/button';
import { useDocumentTitle } from '@/hooks/use-document-title';

export function NotFoundPage() {
  useDocumentTitle('Page not found');

  return (
    <main className="grid min-h-dvh place-items-center px-6">
      <div className="flex flex-col items-center gap-4 text-center">
        <p className="font-mono text-sm text-primary">404</p>
        <h1 className="text-2xl font-semibold tracking-tight">Page not found</h1>
        <p className="text-muted-foreground">The page you are looking for does not exist.</p>
        <Button asChild variant="secondary">
          <Link to="/">Back to home</Link>
        </Button>
      </div>
    </main>
  );
}
