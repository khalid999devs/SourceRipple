package io.sourceripple.shared.api;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),
    ROLE_NOT_ALLOWED(HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED),
    ACCOUNT_DISABLED(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    EMAIL_TAKEN(HttpStatus.CONFLICT),
    DUPLICATE_WATCH(HttpStatus.CONFLICT),
    QUOTA_EXCEEDED(HttpStatus.CONFLICT),
    INVALID_TRANSITION(HttpStatus.CONFLICT),
    ORG_NOT_EMPTY(HttpStatus.CONFLICT),
    TICK_IN_PROGRESS(HttpStatus.CONFLICT),
    INTERVAL_TOO_SHORT(HttpStatus.UNPROCESSABLE_CONTENT),
    SOURCE_FETCH_BLOCKED(HttpStatus.UNPROCESSABLE_CONTENT),
    SOURCE_FETCH_FAILED(HttpStatus.UNPROCESSABLE_CONTENT),
    ROBOTS_DISALLOWED(HttpStatus.UNPROCESSABLE_CONTENT),
    UNSUPPORTED_CONTENT(HttpStatus.UNPROCESSABLE_CONTENT),
    SECTION_NOT_FOUND(HttpStatus.UNPROCESSABLE_CONTENT),
    CHECK_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
