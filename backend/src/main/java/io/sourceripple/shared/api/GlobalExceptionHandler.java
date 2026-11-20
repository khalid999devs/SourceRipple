package io.sourceripple.shared.api;

import io.sourceripple.shared.api.error.ApiException;
import java.util.Comparator;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> handleApi(ApiException ex) {
        return respond(ex.code(), ex.getMessage(), ex.retryable(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleInvalidBody(MethodArgumentNotValidException ex) {
        List<ApiError.FieldErrorDetail> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.FieldErrorDetail(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(ApiError.FieldErrorDetail::field)
                        .thenComparing(ApiError.FieldErrorDetail::message))
                .toList();
        return respond(ErrorCode.VALIDATION_FAILED, "The request has invalid fields.", false, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return respond(ErrorCode.VALIDATION_FAILED, "The request body could not be read.", false, List.of());
    }

    @ExceptionHandler(DisabledException.class)
    ResponseEntity<ApiError> handleDisabled(DisabledException ex) {
        return respond(ErrorCode.ACCOUNT_DISABLED, "This account is disabled.", false, List.of());
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
        return respond(ErrorCode.UNAUTHENTICATED, "Invalid email or password.", false, List.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> handleUnauthenticated(AuthenticationException ex) {
        return respond(ErrorCode.UNAUTHENTICATED, "Authentication is required.", false, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return respond(ErrorCode.FORBIDDEN, "You do not have permission to do this.", false, List.of());
    }

    private static ResponseEntity<ApiError> respond(
            ErrorCode code, String message, boolean retryable, List<ApiError.FieldErrorDetail> fieldErrors) {
        ApiError body = new ApiError(code.name(), message, MDC.get("requestId"), retryable, fieldErrors);
        return ResponseEntity.status(code.status()).body(body);
    }
}
