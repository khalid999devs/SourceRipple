package io.sourceripple.identity.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.sourceripple.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class CsrfProtectionIT extends AbstractIntegrationTest {

    @Test
    void firstMeRequestIssuesAReadableXsrfCookie() throws Exception {
        Cookie token = mvc.perform(get("/api/v1/auth/me")).andReturn().getResponse().getCookie("XSRF-TOKEN");

        assertThat(token).isNotNull();
        assertThat(token.isHttpOnly()).isFalse();
    }

    @Test
    void writeWithoutTheXsrfHeaderIsForbidden() throws Exception {
        Cookie session = registerAndLogin("MAINTAINER");

        mvc.perform(post("/api/v1/auth/logout").cookie(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        mvc.perform(get("/api/v1/auth/me").cookie(session)).andExpect(status().isOk());
    }

    @Test
    void cookieToHeaderRoundTripIsAccepted() throws Exception {
        Cookie token = mvc.perform(get("/api/v1/auth/me")).andReturn().getResponse().getCookie("XSRF-TOKEN");
        String email = uniqueEmail();

        mvc.perform(post("/api/v1/auth/register")
                        .cookie(token)
                        .header("X-XSRF-TOKEN", token.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s", "displayName": "Csrf", "accountType": "REVIEWER"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isCreated());
    }
}
