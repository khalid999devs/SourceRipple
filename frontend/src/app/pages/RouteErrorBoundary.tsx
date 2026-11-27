import { isRouteErrorResponse, useRouteError } from 'react-router';

import { ApiError } from '@/lib/api/api-error';

import { NotFoundPage } from './NotFoundPage';
import { ServerErrorPage } from './ServerErrorPage';

export function RouteErrorBoundary() {
  const error = useRouteError();

  if (isRouteErrorResponse(error) && error.status === 404) {
    return <NotFoundPage />;
  }

  return <ServerErrorPage requestId={error instanceof ApiError ? error.requestId : undefined} />;
}
