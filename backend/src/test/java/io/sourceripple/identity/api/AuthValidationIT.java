package io.sourceripple.identity.api;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.sourceripple.identity.domain.User;
import io.sourceripple.identity.domain.UserRepository;
import io.sourceripple.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AuthValidationIT extends AbstractIntegrationTest {

    @Autowired
    private UserRepository users;

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
    void wrongPasswordOnADisabledAccountGetsTheSameGenericUnauthorized() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD, "MAINTAINER").andExpect(status().isCreated());
        User user = users.findByEmailIgnoreCase(email).orElseThrow();
        user.setEnabled(false);
        users.save(user);

        login(email, "wrong-password-123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
        login(email, PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("ACCOUNT_DISABLED"));
    }

    @Test
    void passwordOverSeventyTwoBytesIsAValidationError() throws Exception {
        register(uniqueEmail(), "é".repeat(37), "MAINTAINER")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"));
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
