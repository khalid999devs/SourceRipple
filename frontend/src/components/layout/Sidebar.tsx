import { PanelLeft } from 'lucide-react';

import { useMediaQuery } from '@/hooks/useMediaQuery';
import { cn } from '@/lib/utils';
import { usePreferencesStore } from '@/stores/preferences-store';

import { SidebarContent } from './sidebar/SidebarContent';
import { SidebarTooltip } from './sidebar/SidebarItem';

export function Sidebar() {
  const preferCollapsed = usePreferencesStore((state) => state.sidebarCollapsed);
  const toggleSidebar = usePreferencesStore((state) => state.toggleSidebar);
  const wide = useMediaQuery('(min-width: 1024px)');
  const collapsed = preferCollapsed || !wide;
  const toggleLabel = collapsed ? 'Expand sidebar' : 'Collapse sidebar';

  return (
    <aside
      className={cn(
        'sticky top-0 hidden h-dvh shrink-0 flex-col border-r bg-surface-1 transition-[width] duration-200 md:flex',
        collapsed ? 'w-16' : 'w-60',
      )}
    >
      <div
        className={cn(
          'flex h-14 shrink-0 items-center border-b px-3',
          collapsed ? 'justify-center' : 'justify-between',
        )}
      >
        {!collapsed && <span className="px-2 font-semibold tracking-tight">SourceRipple</span>}
        {wide && (
          <SidebarTooltip label={toggleLabel} collapsed={collapsed}>
            <button
              type="button"
              onClick={toggleSidebar}
              aria-label={toggleLabel}
              className="grid size-9 place-items-center rounded-md text-muted-foreground transition-colors duration-150 hover:bg-surface-2 hover:text-foreground"
            >
              <PanelLeft aria-hidden className="size-4" />
            </button>
          </SidebarTooltip>
        )}
      </div>
      <SidebarContent collapsed={collapsed} />
    </aside>
  );
}
