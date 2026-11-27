import { Link } from 'react-router';

import { Button } from '@/components/ui/form/button';
import { useDocumentTitle } from '@/hooks/useDocumentTitle';

export function LandingPage() {
  useDocumentTitle('');

  return (
    <section className="mx-auto flex max-w-[1200px] flex-col items-start gap-6 px-6 py-24">
      <p className="font-mono text-xs tracking-wide text-primary uppercase">
        Open source · Agent-ready
      </p>
      <h1 className="max-w-3xl text-5xl font-semibold tracking-tight">
        When the web changes, know which projects it touches.
      </h1>
      <p className="max-w-2xl text-lg text-muted-foreground">
        SourceRipple watches the docs, policies and pages your products rely on, shows exactly what
        changed, and flags the projects that depend on it: in your dashboard, by signed webhook or
        to your AI agents over MCP. No paid AI required.
      </p>
      <div className="flex flex-wrap gap-3">
        <Button asChild size="lg">
          <Link to="/register">Start free</Link>
        </Button>
        <Button asChild size="lg" variant="secondary">
          <Link to="/pricing">View pricing</Link>
        </Button>
      </div>
    </section>
  );
}
