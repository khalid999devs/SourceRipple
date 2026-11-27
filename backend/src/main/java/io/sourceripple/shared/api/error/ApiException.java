package io.sourceripple.shared.api.error;

import io.sourceripple.shared.api.ErrorCode;

public abstract class ApiException extends RuntimeException {

    private final ErrorCode code;

    protected ApiException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }

    public boolean retryable() {
        return false;
    }
}
