package io.sourceripple.source.application.fetch;

import io.sourceripple.shared.api.ErrorCode;
import io.sourceripple.shared.api.error.ApiException;

public class SourceFetchBlockedException extends ApiException {

    public SourceFetchBlockedException(String message) {
        super(ErrorCode.SOURCE_FETCH_BLOCKED, message);
    }
}
