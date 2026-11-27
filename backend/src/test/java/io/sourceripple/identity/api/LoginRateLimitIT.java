package io.sourceripple.identity.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.sourceripple.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class LoginRateLimitIT extends AbstractIntegrationTest {

    private static final int LIMIT = 10;

    @Test
    void eleventhAttemptFromOneAddressIsRateLimited() throws Exception {
        String address = randomAddress();
        for (int attempt = 0; attempt < LIMIT; attempt++) {
            loginFrom(address, uniqueEmail(), "wrong-password-123").andExpect(status().isUnauthorized());
        }

        loginFrom(address, uniqueEmail(), "wrong-password-123")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.retryable").value(true));
    }

    @Test
    void aSpoofedForwardedForHeaderDoesNotResetTheAddressLimit() throws Exception {
        String address = randomAddress();
        for (int attempt = 0; attempt < LIMIT; attempt++) {
            loginForwardedFor(address, randomAddress()).andExpect(status().isUnauthorized());
        }

        loginForwardedFor(address, randomAddress()).andExpect(status().isTooManyRequests());
    }

    @Test
    void eleventhAttemptForOneEmailIsRateLimitedFromAnyAddress() throws Exception {
        String email = uniqueEmail();
        for (int attempt = 0; attempt < LIMIT; attempt++) {
            login(email, "wrong-password-123").andExpect(status().isUnauthorized());
        }

        login(email.toUpperCase(), "wrong-password-123").andExpect(status().isTooManyRequests());
    }

    private ResultActions loginForwardedFor(String address, String forwardedFor) throws Exception {
        return mvc.perform(post("/api/v1/auth/login")
                .with(xsrf())
                .with(request -> {
                    request.setRemoteAddr(address);
                    return request;
                })
                .header("X-Forwarded-For", forwardedFor)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"" + uniqueEmail() + "\", \"password\": \"wrong-password-123\"}"));
    }
}
