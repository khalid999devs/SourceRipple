import createClient, { type Middleware } from 'openapi-fetch';

import { ApiError, networkError, toApiError } from './api-error';
import { CSRF_HEADER, csrfToken, needsCsrfToken } from './csrf';
import type { paths } from './schema';

const csrfMiddleware: Middleware = {
  async onRequest({ request }) {
    if (needsCsrfToken(request.method)) {
      const token = await csrfToken();
      if (token) {
        request.headers.set(CSRF_HEADER, token);
      }
    }
    return request;
  },
};

const errorMiddleware: Middleware = {
  async onResponse({ response }) {
    if (!response.ok) {
      throw await toApiError(response);
    }
    return response;
  },
  onError({ error }) {
    return error instanceof ApiError ? error : networkError();
  },
};

export const api = createClient<paths>({
  baseUrl: '/',
  credentials: 'same-origin',
  headers: { Accept: 'application/json' },
});

api.use(csrfMiddleware, errorMiddleware);
