package io.sourceripple.shared.api.error.status;

import io.sourceripple.shared.api.ErrorCode;
import io.sourceripple.shared.api.error.ApiException;

public class BadRequestException extends ApiException {

    public BadRequestException(ErrorCode code, String message) {
        super(code, message);
    }
}
