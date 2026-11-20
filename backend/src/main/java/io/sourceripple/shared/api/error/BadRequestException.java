package io.sourceripple.shared.api.error;

import io.sourceripple.shared.api.ErrorCode;

public class BadRequestException extends ApiException {

    public BadRequestException(ErrorCode code, String message) {
        super(code, message);
    }
}
