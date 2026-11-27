package io.sourceripple.shared.api;

import io.sourceripple.shared.api.error.ApiException;
import io.sourceripple.shared.api.error.status.RateLimitedException;
import io.sourceripple.shared.infrastructure.RequestIdFilter;
import jakarta.validation.ConstraintViolationException;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RateLimitedException.class)
    ResponseEntity<ApiError> handleRateLimited(RateLimitedException ex) {
        return ResponseEntity.status(ex.code().status())
                .header(HttpHeaders.RETRY_AFTER, Long.toString(ex.retryAfterSeconds()))
                .body(body(ex.code(), ex.getMessage(), true, List.of()));
    }

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
        return invalid("The request has invalid fields.", fieldErrors);
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        HandlerMethodValidationException.class,
        ConstraintViolationException.class,
        MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class
    })
    ResponseEntity<ApiError> handleInvalidRequest(Exception ex) {
        return invalid("The request is invalid.", List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> handleConstraintViolation(DataIntegrityViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMostSpecificCause().getMessage());
        return respond(
                ErrorCode.INVALID_TRANSITION,
                "The request conflicts with a concurrent change. Reload and try again.",
                false,
                List.of());
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

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleOther(Exception ex) {
        if (ex instanceof ErrorResponse errorResponse && errorResponse.getStatusCode().is4xxClientError()) {
            return handleFrameworkClientError(errorResponse.getStatusCode());
        }
        log.error("Unhandled exception", ex);
        return respond(
                ErrorCode.INTERNAL_ERROR,
                "Something went wrong. Quote the request ID if you report it.",
                false,
                List.of());
    }

    private static ResponseEntity<ApiError> handleFrameworkClientError(HttpStatusCode status) {
        if (status.isSameCodeAs(HttpStatus.NOT_FOUND)) {
            return respond(ErrorCode.NOT_FOUND, "The resource was not found.", false, List.of());
        }
        // There are no dedicated codes for 405, 406 or 415, so they keep their status under the request-error code.
        return ResponseEntity.status(status)
                .body(body(ErrorCode.VALIDATION_FAILED, "The request is not supported.", false, List.of()));
    }

    private static ResponseEntity<ApiError> invalid(String message, List<ApiError.FieldErrorDetail> fieldErrors) {
        return respond(ErrorCode.VALIDATION_FAILED, message, false, fieldErrors);
    }

    private static ResponseEntity<ApiError> respond(
            ErrorCode code, String message, boolean retryable, List<ApiError.FieldErrorDetail> fieldErrors) {
        return ResponseEntity.status(code.status()).body(body(code, message, retryable, fieldErrors));
    }

    private static ApiError body(
            ErrorCode code, String message, boolean retryable, List<ApiError.FieldErrorDetail> fieldErrors) {
        return new ApiError(code.name(), message, MDC.get(RequestIdFilter.MDC_KEY), retryable, fieldErrors);
    }
}
