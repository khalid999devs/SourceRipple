import { Check, ChevronsUpDown, Plus } from 'lucide-react';
import { Link } from 'react-router';

import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/overlay/dropdown-menu';
import { useMe } from '@/features/auth/api';
import { cn } from '@/lib/utils';
import { usePreferencesStore } from '@/stores/preferences-store';

interface OrganizationSwitcherProps {
  collapsed: boolean;
}

export function OrganizationSwitcher({ collapsed }: OrganizationSwitcherProps) {
  const { data: me } = useMe();
  const activeOrganizationId = usePreferencesStore((state) => state.activeOrganizationId);
  const setActiveOrganization = usePreferencesStore((state) => state.setActiveOrganization);

  const organizations = me?.organizations ?? [];
  const active =
    organizations.find((organization) => organization.id === activeOrganizationId) ??
    organizations[0];
  const name = active?.name ?? 'No organization';

  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        aria-label={`Organization: ${name}`}
        className={cn(
          'flex h-10 w-full items-center gap-2 rounded-md px-2 text-left text-sm transition-colors duration-150 hover:bg-surface-2',
          collapsed && 'justify-center px-0',
        )}
      >
        <span className="grid size-7 shrink-0 place-items-center rounded-md bg-ripple-gradient text-xs font-semibold text-primary-foreground">
          {name.charAt(0).toUpperCase()}
        </span>
        {!collapsed && (
          <>
            <span className="min-w-0 flex-1 truncate font-medium">{name}</span>
            <ChevronsUpDown aria-hidden className="size-4 shrink-0 text-muted-foreground" />
          </>
        )}
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start" className="w-56">
        <DropdownMenuLabel className="text-xs text-muted-foreground">
          Organizations
        </DropdownMenuLabel>
        {organizations.map((organization) => (
          <DropdownMenuItem
            key={organization.id}
            onSelect={() => setActiveOrganization(organization.id)}
          >
            <span className="min-w-0 flex-1 truncate">{organization.name}</span>
            {organization.id === active?.id && <Check aria-label="Active" />}
          </DropdownMenuItem>
        ))}
        {organizations.length > 0 && <DropdownMenuSeparator />}
        <DropdownMenuItem asChild>
          <Link to="/app/onboarding">
            <Plus aria-hidden />
            New organization
          </Link>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
