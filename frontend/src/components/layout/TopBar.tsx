import { Menu, Plus } from 'lucide-react';
import { Link } from 'react-router';

import { Button } from '@/components/ui/form/button';
import { useMe } from '@/features/auth/api';
import { hasRole } from '@/lib/roles';
import { useUiStore } from '@/stores/ui-store';

import { Breadcrumbs } from './top-bar/Breadcrumbs';
import { ProjectSwitcher } from './top-bar/ProjectSwitcher';
import { SearchTrigger } from './top-bar/SearchTrigger';

export function TopBar() {
  const { data: me } = useMe();
  const setMobileNavOpen = useUiStore((state) => state.setMobileNavOpen);
  const canCreateWatch = hasRole(me?.roles, 'MAINTAINER');

  return (
    <header className="sticky top-0 z-30 flex h-14 shrink-0 items-center gap-3 border-b bg-background/70 px-4 backdrop-blur md:px-6">
      <Button
        variant="ghost"
        size="icon"
        className="md:hidden"
        aria-label="Open navigation"
        onClick={() => setMobileNavOpen(true)}
      >
        <Menu aria-hidden />
      </Button>

      <div className="flex min-w-0 flex-1 items-center gap-3">
        <Breadcrumbs />
        <ProjectSwitcher />
      </div>

      <div className="flex flex-1 items-center justify-end gap-3">
        <SearchTrigger />
        {canCreateWatch && (
          <Button asChild size="sm">
            <Link to="/app/watches/new">
              <Plus aria-hidden />
              New watch
            </Link>
          </Button>
        )}
      </div>
    </header>
  );
}
