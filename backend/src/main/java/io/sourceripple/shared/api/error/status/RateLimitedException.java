package io.sourceripple.shared.api.error.status;

import io.sourceripple.shared.api.ErrorCode;
import io.sourceripple.shared.api.error.ApiException;
import java.time.Duration;

public class RateLimitedException extends ApiException {

    private final Duration retryAfter;

    public RateLimitedException(ErrorCode code, String message, Duration retryAfter) {
        super(code, message);
        this.retryAfter = retryAfter;
    }

    public long retryAfterSeconds() {
        return Math.max(1, (retryAfter.toMillis() + 999) / 1000);
    }

    @Override
    public boolean retryable() {
        return true;
    }
}
