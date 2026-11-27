import { ChevronRight } from 'lucide-react';
import { Link, useMatches } from 'react-router';

export interface CrumbHandle {
  crumb: string;
}

function isCrumbHandle(handle: unknown): handle is CrumbHandle {
  return (
    typeof handle === 'object' &&
    handle !== null &&
    'crumb' in handle &&
    typeof handle.crumb === 'string'
  );
}

export function Breadcrumbs() {
  const crumbs = useMatches().flatMap((match) =>
    isCrumbHandle(match.handle) ? [{ path: match.pathname, label: match.handle.crumb }] : [],
  );

  return (
    <nav aria-label="Breadcrumb" className="min-w-0">
      <ol className="flex min-w-0 items-center gap-1.5 text-sm text-muted-foreground">
        {crumbs.map((crumb, index) => {
          const last = index === crumbs.length - 1;
          return (
            <li key={crumb.path} className="flex min-w-0 items-center gap-1.5">
              {index > 0 && <ChevronRight aria-hidden className="size-3.5 shrink-0" />}
              {last ? (
                <span aria-current="page" className="truncate font-medium text-foreground">
                  {crumb.label}
                </span>
              ) : (
                <Link to={crumb.path} className="truncate transition-colors hover:text-foreground">
                  {crumb.label}
                </Link>
              )}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
