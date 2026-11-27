export interface FieldErrorDetail {
  field: string;
  message: string;
}

export interface ErrorEnvelope {
  error: string;
  message: string;
  requestId?: string;
  retryable: boolean;
  fieldErrors: FieldErrorDetail[];
}

export class ApiError extends Error {
  readonly status: number;
  readonly error: string;
  readonly requestId?: string;
  readonly retryable: boolean;
  readonly fieldErrors: FieldErrorDetail[];

  constructor(status: number, envelope: ErrorEnvelope) {
    super(envelope.message);
    this.name = 'ApiError';
    this.status = status;
    this.error = envelope.error;
    this.requestId = envelope.requestId;
    this.retryable = envelope.retryable;
    this.fieldErrors = envelope.fieldErrors;
  }
}

const FALLBACK_CODES: Record<number, string> = {
  400: 'VALIDATION_FAILED',
  401: 'UNAUTHENTICATED',
  403: 'FORBIDDEN',
  404: 'NOT_FOUND',
  429: 'RATE_LIMITED',
};

function isEnvelope(body: unknown): body is ErrorEnvelope {
  return (
    typeof body === 'object' &&
    body !== null &&
    'error' in body &&
    typeof body.error === 'string' &&
    'message' in body &&
    typeof body.message === 'string'
  );
}

export async function toApiError(response: Response): Promise<ApiError> {
  const headerRequestId = response.headers.get('X-Request-Id') ?? undefined;
  const body: unknown = await response
    .clone()
    .json()
    .catch(() => null);

  if (isEnvelope(body)) {
    return new ApiError(response.status, {
      error: body.error,
      message: body.message,
      requestId: body.requestId ?? headerRequestId,
      retryable: Boolean(body.retryable),
      fieldErrors: Array.isArray(body.fieldErrors) ? body.fieldErrors : [],
    });
  }

  return new ApiError(response.status, {
    error: FALLBACK_CODES[response.status] ?? 'INTERNAL_ERROR',
    message: 'The server could not complete the request.',
    requestId: headerRequestId,
    retryable: response.status >= 500,
    fieldErrors: [],
  });
}

export function networkError(): ApiError {
  return new ApiError(0, {
    error: 'NETWORK_ERROR',
    message: 'Could not reach the server. Check your connection and try again.',
    retryable: true,
    fieldErrors: [],
  });
}
