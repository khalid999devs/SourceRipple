package io.sourceripple.shared.api.error;

import io.sourceripple.shared.api.ErrorCode;

public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, message);
    }
}
