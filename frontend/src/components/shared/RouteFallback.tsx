export function RouteFallback() {
  return (
    <div role="status" aria-live="polite" className="grid min-h-dvh place-items-center">
      <span className="text-sm text-muted-foreground">Loading…</span>
    </div>
  );
}
