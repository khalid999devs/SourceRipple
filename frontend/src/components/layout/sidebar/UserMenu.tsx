import { ChevronsUpDown, UserRound } from 'lucide-react';
import { Link } from 'react-router';

import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuTrigger,
} from '@/components/ui/overlay/dropdown-menu';
import { useMe } from '@/features/auth/api';
import { cn } from '@/lib/utils';

interface UserMenuProps {
  collapsed: boolean;
}

function initialsOf(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('');
}

export function UserMenu({ collapsed }: UserMenuProps) {
  const { data: me } = useMe();
  const displayName = me?.user.displayName ?? '';
  const email = me?.user.email ?? '';

  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        aria-label="Account menu"
        className={cn(
          'flex h-11 w-full items-center gap-2 rounded-md px-2 text-left text-sm transition-colors duration-150 hover:bg-surface-2',
          collapsed && 'justify-center px-0',
        )}
      >
        <span className="grid size-7 shrink-0 place-items-center rounded-full border bg-surface-2 text-xs font-medium">
          {initialsOf(displayName) || <UserRound aria-hidden className="size-4" />}
        </span>
        {!collapsed && (
          <>
            <span className="flex min-w-0 flex-1 flex-col leading-tight">
              <span className="truncate font-medium">{displayName}</span>
              <span className="truncate text-xs text-muted-foreground">{email}</span>
            </span>
            <ChevronsUpDown aria-hidden className="size-4 shrink-0 text-muted-foreground" />
          </>
        )}
      </DropdownMenuTrigger>
      <DropdownMenuContent side="top" align="start" className="w-56">
        <DropdownMenuLabel className="flex flex-col gap-0.5">
          <span className="truncate">{displayName}</span>
          <span className="truncate text-xs font-normal text-muted-foreground">{email}</span>
        </DropdownMenuLabel>
        <DropdownMenuItem asChild>
          <Link to="/app/settings">
            <UserRound aria-hidden />
            Profile and settings
          </Link>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
