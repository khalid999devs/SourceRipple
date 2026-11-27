package io.sourceripple.shared.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void aConstraintViolationIsAConflictWithTheEnvelope() {
        DataIntegrityViolationException race =
                new DataIntegrityViolationException("duplicate key value violates organization_members_pkey");

        ResponseEntity<ApiError> response = handler.handleConstraintViolation(race);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().error()).isEqualTo(ErrorCode.INVALID_TRANSITION.name());
        assertThat(response.getBody().retryable()).isFalse();
        assertThat(response.getBody().message()).doesNotContain("organization_members");
    }
}
