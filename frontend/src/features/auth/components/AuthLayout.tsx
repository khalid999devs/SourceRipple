import type { ReactNode } from 'react';
import { Link, Navigate, useSearchParams } from 'react-router';

import { useMe } from '@/features/auth/api';
import { safeNextPath } from '@/features/auth/next-path';

const HIGHLIGHTS = [
  'Watch the docs, policies and pages your products rely on.',
  'See exactly what changed, section by section.',
  'Get flagged impacts in the dashboard, by signed webhook or over MCP.',
];

interface AuthLayoutProps {
  title: string;
  description: string;
  footer: ReactNode;
  children: ReactNode;
}

export function AuthLayout({ title, description, footer, children }: AuthLayoutProps) {
  const [searchParams] = useSearchParams();
  const { data: me } = useMe();

  if (me) {
    return <Navigate to={safeNextPath(searchParams.get('next'))} replace />;
  }

  return (
    <div className="grid flex-1 lg:grid-cols-2">
      <main className="flex flex-col px-6 py-8 sm:px-10">
        <Link to="/" className="flex w-fit items-center gap-2 font-semibold tracking-tight">
          <span aria-hidden className="size-5 rounded-full bg-ripple-gradient" />
          SourceRipple
        </Link>

        <div className="mx-auto flex w-full max-w-sm flex-1 flex-col justify-center gap-8 py-12">
          <header className="flex flex-col gap-2">
            <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
            <p className="text-muted-foreground">{description}</p>
          </header>
          {children}
          <p className="text-sm text-muted-foreground">{footer}</p>
        </div>
      </main>

      <aside className="relative hidden overflow-hidden border-l bg-surface-1 lg:flex">
        <div aria-hidden className="absolute inset-0 bg-dot-grid opacity-60" />
        <div aria-hidden className="absolute inset-0 bg-ripple-glow" />
        <div className="relative m-auto flex max-w-md flex-col gap-6 px-10">
          <p className="font-mono text-xs tracking-wide text-primary uppercase">
            Open source · Agent-ready
          </p>
          <h2 className="text-3xl font-semibold tracking-tight">
            When the web changes, know which projects it touches.
          </h2>
          <ul className="flex flex-col gap-3 text-muted-foreground">
            {HIGHLIGHTS.map((highlight) => (
              <li key={highlight} className="flex gap-3">
                <span aria-hidden className="mt-2 size-1.5 shrink-0 rounded-full bg-primary" />
                {highlight}
              </li>
            ))}
          </ul>
        </div>
      </aside>
    </div>
  );
}
