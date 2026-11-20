package io.sourceripple.shared.api.error;

import io.sourceripple.shared.api.ErrorCode;

public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }
}
