package io.sourceripple.content.application;

import io.sourceripple.shared.api.ErrorCode;
import io.sourceripple.shared.api.error.ApiException;

public class UnsupportedContentException extends ApiException {

    public UnsupportedContentException(String message) {
        super(ErrorCode.UNSUPPORTED_CONTENT, message);
    }
}
