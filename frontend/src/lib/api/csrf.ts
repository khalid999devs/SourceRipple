const CSRF_COOKIE = 'XSRF-TOKEN';
const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS', 'TRACE']);

export const CSRF_HEADER = 'X-XSRF-TOKEN';

function readCookie(name: string): string | undefined {
  const prefix = `${name}=`;
  const value = document.cookie
    .split('; ')
    .find((entry) => entry.startsWith(prefix))
    ?.slice(prefix.length);
  return value === undefined ? undefined : decodeURIComponent(value);
}

export function needsCsrfToken(method: string): boolean {
  return !SAFE_METHODS.has(method.toUpperCase());
}

export async function csrfToken(): Promise<string | undefined> {
  const token = readCookie(CSRF_COOKIE);
  if (token) {
    return token;
  }

  // The backend sets the cookie on any API response, and logout clears it.
  await fetch('/api/v1/auth/me', { credentials: 'same-origin' }).catch(() => undefined);
  return readCookie(CSRF_COOKIE);
}
