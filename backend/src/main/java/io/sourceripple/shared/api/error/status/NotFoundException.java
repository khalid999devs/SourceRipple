package io.sourceripple.shared.api.error.status;

import io.sourceripple.shared.api.ErrorCode;
import io.sourceripple.shared.api.error.ApiException;

public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }
}
