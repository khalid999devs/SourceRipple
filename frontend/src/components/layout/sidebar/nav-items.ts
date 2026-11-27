import {
  FolderKanban,
  GitCompare,
  Globe,
  Inbox,
  LayoutDashboard,
  Network,
  Plug,
  Shield,
  type LucideIcon,
} from 'lucide-react';

import type { Role } from '@/lib/roles';

export interface NavItem {
  to: string;
  label: string;
  icon: LucideIcon;
  end?: boolean;
  role?: Role;
}

export const MAIN_NAV: readonly NavItem[] = [
  { to: '/app', label: 'Overview', icon: LayoutDashboard, end: true },
  { to: '/app/projects', label: 'Projects', icon: FolderKanban },
  { to: '/app/watches', label: 'Sources', icon: Globe },
  { to: '/app/changes', label: 'Changes', icon: GitCompare },
  { to: '/app/dependencies', label: 'Dependency Map', icon: Network },
  { to: '/app/impacts', label: 'Impact Inbox', icon: Inbox },
  { to: '/app/integrations', label: 'Integrations', icon: Plug },
  { to: '/app/admin', label: 'Admin', icon: Shield, role: 'ADMIN' },
];

export const GITHUB_URL = 'https://github.com/khalid999devs/SourceRipple';
