package io.sourceripple.source.application.fetch.failure;

import io.sourceripple.shared.api.ErrorCode;
import io.sourceripple.shared.api.error.ApiException;

public class SourceFetchFailedException extends ApiException {

    private final boolean retryable;
    private final Integer httpStatus;

    public SourceFetchFailedException(String message, boolean retryable, Integer httpStatus) {
        super(ErrorCode.SOURCE_FETCH_FAILED, message);
        this.retryable = retryable;
        this.httpStatus = httpStatus;
    }

    public static SourceFetchFailedException permanent(String message) {
        return new SourceFetchFailedException(message, false, null);
    }

    public static SourceFetchFailedException retryable(String message) {
        return new SourceFetchFailedException(message, true, null);
    }

    @Override
    public boolean retryable() {
        return retryable;
    }

    public Integer httpStatus() {
        return httpStatus;
    }
}
