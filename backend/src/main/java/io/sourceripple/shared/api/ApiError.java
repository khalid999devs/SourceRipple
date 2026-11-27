package io.sourceripple.shared.api;

import java.util.List;

public record ApiError(
        String error, String message, String requestId, boolean retryable, List<FieldErrorDetail> fieldErrors) {

    public record FieldErrorDetail(String field, String message) {}
}
