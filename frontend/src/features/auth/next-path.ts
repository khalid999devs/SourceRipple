const DEFAULT_PATH = '/app';

export function safeNextPath(next: string | null): string {
  if (!next || !next.startsWith('/') || next.startsWith('//') || next.startsWith('/\\')) {
    return DEFAULT_PATH;
  }
  return next;
}
