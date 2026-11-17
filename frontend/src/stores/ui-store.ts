import { create } from 'zustand';

interface UiState {
  commandPaletteOpen: boolean;
  shortcutSheetOpen: boolean;
  mobileNavOpen: boolean;
  setCommandPaletteOpen: (open: boolean) => void;
  setShortcutSheetOpen: (open: boolean) => void;
  setMobileNavOpen: (open: boolean) => void;
}

export const useUiStore = create<UiState>()((set) => ({
  commandPaletteOpen: false,
  shortcutSheetOpen: false,
  mobileNavOpen: false,
  setCommandPaletteOpen: (commandPaletteOpen) => set({ commandPaletteOpen }),
  setShortcutSheetOpen: (shortcutSheetOpen) => set({ shortcutSheetOpen }),
  setMobileNavOpen: (mobileNavOpen) => set({ mobileNavOpen }),
}));
