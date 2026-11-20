package io.sourceripple.identity.api;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.sourceripple.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class AuthValidationIT extends AbstractIntegrationTest {

    @Test
    void wrongPasswordAndUnknownEmailGetTheSameGenericUnauthorized() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD, "MAINTAINER").andExpect(status().isCreated());

        login(email, "wrong-password-123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
        login(uniqueEmail(), PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    void invalidRegisterBodyReturnsFieldErrors() throws Exception {
        register("not-an-email", "short", "MAINTAINER")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.retryable").value(false))
                .andExpect(jsonPath("$.fieldErrors[*].field").value(hasItems("email", "password")));
    }

    @Test
    void adminAccountTypeIsRejected() throws Exception {
        register(uniqueEmail(), PASSWORD, "ADMIN")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ROLE_NOT_ALLOWED"));
    }

    @Test
    void duplicateEmailInAnotherCaseIsAConflict() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD, "MAINTAINER").andExpect(status().isCreated());

        register(email.toUpperCase(), PASSWORD, "REVIEWER")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("EMAIL_TAKEN"));
    }
}
