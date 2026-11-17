import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';

export type DiffViewMode = 'split' | 'unified';

export interface InboxFilters {
  projectId?: string;
  status?: string;
  confidence?: string;
  sourceId?: string;
}

interface PreferencesState {
  theme: 'dark';
  sidebarCollapsed: boolean;
  activeOrganizationId: string | null;
  activeProjectId: string | null;
  diffViewMode: DiffViewMode;
  inboxFilters: InboxFilters;
  toggleSidebar: () => void;
  setActiveOrganization: (organizationId: string | null) => void;
  setActiveProject: (projectId: string | null) => void;
  setDiffViewMode: (mode: DiffViewMode) => void;
  setInboxFilters: (filters: InboxFilters) => void;
}

export const usePreferencesStore = create<PreferencesState>()(
  persist(
    (set) => ({
      theme: 'dark',
      sidebarCollapsed: false,
      activeOrganizationId: null,
      activeProjectId: null,
      diffViewMode: 'split',
      inboxFilters: {},
      toggleSidebar: () => set((state) => ({ sidebarCollapsed: !state.sidebarCollapsed })),
      setActiveOrganization: (activeOrganizationId) =>
        set({ activeOrganizationId, activeProjectId: null }),
      setActiveProject: (activeProjectId) => set({ activeProjectId }),
      setDiffViewMode: (diffViewMode) => set({ diffViewMode }),
      setInboxFilters: (inboxFilters) => set({ inboxFilters }),
    }),
    {
      name: 'sourceripple.preferences',
      version: 1,
      storage: createJSONStorage(() => localStorage),
    },
  ),
);
