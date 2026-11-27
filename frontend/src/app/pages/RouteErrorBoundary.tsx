import { isRouteErrorResponse, useRouteError } from 'react-router';

import { NotFoundPage } from './NotFoundPage';
import { ServerErrorPage } from './ServerErrorPage';

export function RouteErrorBoundary() {
  const error = useRouteError();

  if (isRouteErrorResponse(error) && error.status === 404) {
    return <NotFoundPage />;
  }

  return <ServerErrorPage />;
}
