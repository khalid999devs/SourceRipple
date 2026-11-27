import { Search } from 'lucide-react';

import { useUiStore } from '@/stores/ui-store';

export function SearchTrigger() {
  const setCommandPaletteOpen = useUiStore((state) => state.setCommandPaletteOpen);

  return (
    <button
      type="button"
      onClick={() => setCommandPaletteOpen(true)}
      className="flex h-9 w-full max-w-xs items-center gap-2 rounded-md border bg-surface-1 px-3 text-sm text-muted-foreground transition-colors duration-150 hover:text-foreground"
    >
      <Search aria-hidden className="size-4 shrink-0" />
      <span className="truncate">Search…</span>
      <kbd className="ml-auto rounded border bg-surface-2 px-1.5 font-mono text-xs">⌘K</kbd>
    </button>
  );
}
