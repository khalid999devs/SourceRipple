package io.sourceripple.identity.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.sourceripple.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.RequestBuilder;

class UnauthenticatedAccessIT extends AbstractIntegrationTest {

    @Test
    void protectedEndpointsWithoutASessionReturnTheUnauthenticatedEnvelope() throws Exception {
        expectUnauthenticated(get("/api/v1/auth/me"));
        expectUnauthenticated(get("/api/v1/organizations"));
        expectUnauthenticated(get("/api/v1/admin/users"));
        expectUnauthenticated(post("/api/v1/organizations").with(xsrf()));
        expectUnauthenticated(delete("/api/v1/projects/00000000-0000-0000-0000-000000000000").with(xsrf()));
    }

    @Test
    void adminPathsAreForbiddenForNonAdmins() throws Exception {
        mvc.perform(get("/api/v1/admin/users").cookie(registerAndLogin("MAINTAINER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    private void expectUnauthenticated(RequestBuilder request) throws Exception {
        mvc.perform(request)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.retryable").value(false))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
