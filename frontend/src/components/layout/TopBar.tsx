import { Plus, Search } from 'lucide-react';
import { Link } from 'react-router';

import { Button } from '@/components/ui/button';
import { useUiStore } from '@/stores/ui-store';

export function TopBar() {
  const setCommandPaletteOpen = useUiStore((state) => state.setCommandPaletteOpen);

  return (
    <header className="sticky top-0 z-30 flex h-14 items-center justify-between gap-4 border-b bg-background/70 px-6 backdrop-blur">
      <button
        type="button"
        onClick={() => setCommandPaletteOpen(true)}
        className="flex h-9 w-full max-w-sm items-center gap-2 rounded-md border border-border bg-surface-1 px-3 text-sm text-muted-foreground hover:text-foreground"
      >
        <Search aria-hidden className="size-4" />
        <span>Search…</span>
        <kbd className="ml-auto font-mono text-xs">⌘K</kbd>
      </button>
      <Button asChild size="sm">
        <Link to="/app/watches/new">
          <Plus aria-hidden />
          New watch
        </Link>
      </Button>
    </header>
  );
}
