package io.sourceripple.shared.api.error;

import io.sourceripple.shared.api.ErrorCode;

public class ConflictException extends ApiException {

    public ConflictException(ErrorCode code, String message) {
        super(code, message);
    }
}
