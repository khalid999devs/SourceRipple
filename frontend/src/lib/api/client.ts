import createClient, { type Middleware } from 'openapi-fetch';

import type { paths } from './schema';

const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS', 'TRACE']);

function readCookie(name: string): string | undefined {
  const prefix = `${name}=`;
  return document.cookie
    .split('; ')
    .find((entry) => entry.startsWith(prefix))
    ?.slice(prefix.length);
}

const csrfMiddleware: Middleware = {
  onRequest({ request }) {
    if (!SAFE_METHODS.has(request.method)) {
      const token = readCookie('XSRF-TOKEN');
      if (token) {
        request.headers.set('X-XSRF-TOKEN', decodeURIComponent(token));
      }
    }
    return request;
  },
};

export const api = createClient<paths>({
  baseUrl: '/',
  credentials: 'same-origin',
  headers: { Accept: 'application/json' },
});

api.use(csrfMiddleware);
