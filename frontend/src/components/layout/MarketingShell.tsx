import { Link, NavLink, Outlet } from 'react-router';

import { Button } from '@/components/ui/form/button';

const NAV_LINKS = [{ to: '/pricing', label: 'Pricing' }] as const;

export function MarketingShell() {
  return (
    <div className="flex flex-1 flex-col">
      <header className="sticky top-0 z-40 border-b bg-background/70 backdrop-blur">
        <nav
          aria-label="Main"
          className="mx-auto flex h-14 max-w-[1200px] items-center justify-between px-6"
        >
          <Link to="/" className="font-semibold tracking-tight">
            SourceRipple
          </Link>
          <div className="flex items-center gap-2">
            {NAV_LINKS.map((link) => (
              <Button key={link.to} asChild variant="ghost" size="sm">
                <NavLink to={link.to}>{link.label}</NavLink>
              </Button>
            ))}
            <Button asChild variant="ghost" size="sm">
              <Link to="/login">Log in</Link>
            </Button>
            <Button asChild size="sm">
              <Link to="/register">Start free</Link>
            </Button>
          </div>
        </nav>
      </header>
      <main className="flex-1">
        <Outlet />
      </main>
      <footer className="border-t">
        <div className="mx-auto flex max-w-[1200px] flex-wrap items-center justify-between gap-4 px-6 py-8 text-sm text-muted-foreground">
          <span>SourceRipple · Apache-2.0 · Public preview</span>
          <div className="flex gap-4">
            <Link to="/privacy" className="hover:text-foreground">
              Privacy
            </Link>
            <Link to="/terms" className="hover:text-foreground">
              Terms
            </Link>
          </div>
        </div>
      </footer>
    </div>
  );
}
