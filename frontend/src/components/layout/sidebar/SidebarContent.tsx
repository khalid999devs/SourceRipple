import { Bell, GitFork } from 'lucide-react';

import { useMe } from '@/features/auth/api';
import { hasRole } from '@/lib/roles';

import { GITHUB_URL, MAIN_NAV } from './nav-items';
import { OrganizationSwitcher } from './OrganizationSwitcher';
import { SidebarItem } from './SidebarItem';
import { UserMenu } from './UserMenu';

interface SidebarContentProps {
  collapsed: boolean;
  onNavigate?: () => void;
}

export function SidebarContent({ collapsed, onNavigate }: SidebarContentProps) {
  const { data: me } = useMe();
  const items = MAIN_NAV.filter((item) => !item.role || hasRole(me?.roles, item.role));

  return (
    <>
      <div className="border-b p-2">
        <OrganizationSwitcher collapsed={collapsed} />
      </div>

      <nav aria-label="Main" className="flex flex-1 flex-col gap-1 overflow-y-auto p-2">
        {items.map((item) => (
          <SidebarItem
            key={item.to}
            to={item.to}
            label={item.label}
            icon={item.icon}
            end={item.end}
            collapsed={collapsed}
            onNavigate={onNavigate}
          />
        ))}
      </nav>

      <div className="flex flex-col gap-1 border-t p-2">
        <SidebarItem
          to="/app/notifications"
          label="Notifications"
          icon={Bell}
          collapsed={collapsed}
          onNavigate={onNavigate}
        />
        <SidebarItem to={GITHUB_URL} label="GitHub" icon={GitFork} collapsed={collapsed} external />
        <UserMenu collapsed={collapsed} />
      </div>
    </>
  );
}
