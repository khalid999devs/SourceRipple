import {
  Bell,
  FolderKanban,
  GitCompare,
  Globe,
  Inbox,
  LayoutDashboard,
  Network,
  PanelLeft,
  Plug,
  Settings,
  Shield,
  type LucideIcon,
} from 'lucide-react';
import { NavLink } from 'react-router';

import { cn } from '@/lib/utils';
import { usePreferencesStore } from '@/stores/preferences-store';

interface NavItem {
  to: string;
  label: string;
  icon: LucideIcon;
  end?: boolean;
}

const MAIN_NAV: readonly NavItem[] = [
  { to: '/app', label: 'Overview', icon: LayoutDashboard, end: true },
  { to: '/app/projects', label: 'Projects', icon: FolderKanban },
  { to: '/app/watches', label: 'Sources', icon: Globe },
  { to: '/app/changes', label: 'Changes', icon: GitCompare },
  { to: '/app/dependencies', label: 'Dependency Map', icon: Network },
  { to: '/app/impacts', label: 'Impact Inbox', icon: Inbox },
  { to: '/app/integrations', label: 'Integrations', icon: Plug },
  { to: '/app/admin', label: 'Admin', icon: Shield },
];

const FOOTER_NAV: readonly NavItem[] = [
  { to: '/app/notifications', label: 'Notifications', icon: Bell },
  { to: '/app/settings', label: 'Settings', icon: Settings },
];

function SidebarLink({ item, collapsed }: { item: NavItem; collapsed: boolean }) {
  const Icon = item.icon;
  return (
    <NavLink
      to={item.to}
      end={item.end}
      title={collapsed ? item.label : undefined}
      className={({ isActive }) =>
        cn(
          'flex h-9 items-center gap-3 rounded-md px-3 text-sm text-muted-foreground transition-colors duration-150 hover:bg-surface-2 hover:text-foreground',
          isActive && 'bg-surface-2 text-foreground',
        )
      }
    >
      <Icon aria-hidden className="size-4 shrink-0" />
      <span className={cn(collapsed && 'sr-only')}>{item.label}</span>
    </NavLink>
  );
}

export function Sidebar() {
  const collapsed = usePreferencesStore((state) => state.sidebarCollapsed);
  const toggleSidebar = usePreferencesStore((state) => state.toggleSidebar);

  return (
    <aside
      className={cn(
        'flex shrink-0 flex-col border-r bg-surface-1 transition-[width] duration-200',
        collapsed ? 'w-16' : 'w-60',
      )}
    >
      <div className="flex h-14 items-center justify-between border-b px-3">
        {!collapsed && <span className="px-2 font-semibold tracking-tight">SourceRipple</span>}
        <button
          type="button"
          onClick={toggleSidebar}
          aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          className="grid size-9 place-items-center rounded-md text-muted-foreground hover:bg-surface-2 hover:text-foreground"
        >
          <PanelLeft aria-hidden className="size-4" />
        </button>
      </div>
      <nav aria-label="Main" className="flex flex-1 flex-col gap-1 p-2">
        {MAIN_NAV.map((item) => (
          <SidebarLink key={item.to} item={item} collapsed={collapsed} />
        ))}
      </nav>
      <nav aria-label="Account" className="flex flex-col gap-1 border-t p-2">
        {FOOTER_NAV.map((item) => (
          <SidebarLink key={item.to} item={item} collapsed={collapsed} />
        ))}
      </nav>
    </aside>
  );
}
