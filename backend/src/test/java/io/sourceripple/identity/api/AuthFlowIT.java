package io.sourceripple.identity.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.sourceripple.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class AuthFlowIT extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void registerLoginMeLogoutThenMeIsUnauthorized() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD, "MAINTAINER")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/auth/me"))
                .andExpect(jsonPath("$.email").value(email));

        Cookie session = sessionCookie(login(email, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.roles[0]").value("MAINTAINER")));

        mvc.perform(get("/api/v1/auth/me").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.lastLoginAt").isNotEmpty())
                .andExpect(jsonPath("$.roles[0]").value("MAINTAINER"));

        mvc.perform(post("/api/v1/auth/logout").with(xsrf()).cookie(session)).andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/auth/me").cookie(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"));
    }

    @Test
    void storesTheSessionInPostgresAndOnlyABcryptHash() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD, "REVIEWER").andExpect(status().isCreated());
        login(email, PASSWORD).andExpect(status().isOk());

        Integer sessions = jdbc.queryForObject(
                "SELECT count(*) FROM spring_session WHERE principal_name = ?", Integer.class, email);
        String hash = jdbc.queryForObject("SELECT password_hash FROM users WHERE email = ?", String.class, email);

        assertThat(sessions).isEqualTo(1);
        assertThat(hash).startsWith("$2").doesNotContain(PASSWORD);
    }

    @Test
    void loginRotatesTheSessionId() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD, "MAINTAINER").andExpect(status().isCreated());
        Cookie first = sessionCookie(login(email, PASSWORD));

        Cookie second = sessionCookie(mvc.perform(post("/api/v1/auth/login")
                        .with(xsrf())
                        .cookie(first)
                        .contentType("application/json")
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk()));

        assertThat(second.getValue()).isNotEqualTo(first.getValue());
        mvc.perform(get("/api/v1/auth/me").cookie(first)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me").cookie(second)).andExpect(status().isOk());
    }
}
