import type { LucideIcon } from 'lucide-react';
import type { ReactElement } from 'react';
import { NavLink } from 'react-router';

import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/overlay/tooltip';
import { cn } from '@/lib/utils';

const ITEM_CLASSES =
  'flex h-9 items-center gap-3 rounded-md px-3 text-sm text-muted-foreground transition-colors duration-150 hover:bg-surface-2 hover:text-foreground';

interface SidebarTooltipProps {
  label: string;
  collapsed: boolean;
  children: ReactElement;
}

export function SidebarTooltip({ label, collapsed, children }: SidebarTooltipProps) {
  if (!collapsed) {
    return children;
  }

  return (
    <Tooltip>
      <TooltipTrigger asChild>{children}</TooltipTrigger>
      <TooltipContent side="right">{label}</TooltipContent>
    </Tooltip>
  );
}

interface SidebarItemProps {
  label: string;
  icon: LucideIcon;
  collapsed: boolean;
  to: string;
  external?: boolean;
  end?: boolean;
  onNavigate?: () => void;
}

export function SidebarItem({
  label,
  icon: Icon,
  collapsed,
  to,
  external = false,
  end,
  onNavigate,
}: SidebarItemProps) {
  const content = (
    <>
      <Icon aria-hidden className="size-4 shrink-0" />
      <span className={cn('truncate', collapsed && 'sr-only')}>{label}</span>
    </>
  );

  return (
    <SidebarTooltip label={label} collapsed={collapsed}>
      {external ? (
        <a href={to} target="_blank" rel="noreferrer" className={ITEM_CLASSES}>
          {content}
        </a>
      ) : (
        <NavLink
          to={to}
          end={end}
          onClick={onNavigate}
          className={({ isActive }) =>
            cn(ITEM_CLASSES, isActive && 'bg-surface-2 font-medium text-foreground')
          }
        >
          {content}
        </NavLink>
      )}
    </SidebarTooltip>
  );
}
